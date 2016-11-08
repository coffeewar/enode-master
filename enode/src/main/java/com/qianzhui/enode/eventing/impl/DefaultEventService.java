package com.qianzhui.enode.eventing.impl;

import com.qianzhui.enode.ENode;
import com.qianzhui.enode.commanding.*;
import com.qianzhui.enode.common.io.IOHelper;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.domain.IMemoryCache;
import com.qianzhui.enode.eventing.*;
import com.qianzhui.enode.infrastructure.IMessagePublisher;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by junbo_xu on 2016/3/31.
 */
public class DefaultEventService implements IEventService {
    private IProcessingCommandHandler _processingCommandHandler;
    private final ConcurrentMap<String, EventMailBox> _eventMailboxDict;
    private final IMemoryCache _memoryCache;
    private final IEventStore _eventStore;
    private final IMessagePublisher<DomainEventStreamMessage> _domainEventPublisher;
    private final IOHelper _ioHelper;
    private final ILogger _logger;
    private final int _batchSize;

    @Inject
    public DefaultEventService(
            IMemoryCache memoryCache,
            IEventStore eventStore,
            IMessagePublisher<DomainEventStreamMessage> domainEventPublisher,
            IOHelper ioHelper,
            ILoggerFactory loggerFactory) {
        _eventMailboxDict = new ConcurrentHashMap<>();
        _ioHelper = ioHelper;
        _memoryCache = memoryCache;
        _eventStore = eventStore;
        _domainEventPublisher = domainEventPublisher;
        _logger = loggerFactory.create(getClass());
        _batchSize = ENode.getInstance().getSetting().getEventMailBoxPersistenceMaxBatchSize();
    }

    @Override
    public void setProcessingCommandHandler(IProcessingCommandHandler processingCommandHandler) {
        _processingCommandHandler = processingCommandHandler;
    }

    @Override
    public void commitDomainEventAsync(EventCommittingContext context) {
        EventMailBox eventMailbox = _eventMailboxDict.computeIfAbsent(context.getAggregateRoot().uniqueId(), x ->
                new EventMailBox(x, _batchSize, committingContexts ->
                {
                    if (committingContexts == null || committingContexts.size() == 0) {
                        return;
                    }
                    if (_eventStore.isSupportBatchAppendEvent()) {
                        batchPersistEventAsync(committingContexts, 0);
                    } else {
                        persistEventOneByOne(committingContexts);
                    }
                })
        );

        eventMailbox.enqueueMessage(context);
        refreshAggregateMemoryCache(context);
        context.getProcessingCommand().getMailbox().tryExecuteNextMessage();
    }

    @Override
    public void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStream eventStream) {
        if (eventStream.items() == null || eventStream.items().size() == 0) {
            eventStream.setItems(processingCommand.getItems());
        }
        DomainEventStreamMessage eventStreamMessage = new DomainEventStreamMessage(processingCommand.getMessage().id(), eventStream.aggregateRootId(),
                eventStream.version(), eventStream.aggregateRootTypeName(), eventStream.events(), eventStream.items());
        publishDomainEventAsync(processingCommand, eventStreamMessage, 0);
    }

    private void batchPersistEventAsync(List<EventCommittingContext> committingContexts, int retryTimes) {
        _ioHelper.tryAsyncActionRecursively("BatchPersistEventAsync",
                () -> _eventStore.batchAppendAsync(committingContexts.stream().map(x -> x.getEventStream()).collect(Collectors.toList())),
                currentRetryTimes -> batchPersistEventAsync(committingContexts, currentRetryTimes),
                result ->
                {
                    EventMailBox eventMailBox = committingContexts.get(0).getEventMailBox();
                    EventAppendResult appendResult = result.getData();
                    if (appendResult == EventAppendResult.Success) {
                        if (_logger.isDebugEnabled()) {
                            _logger.debug("Batch persist event success, aggregateRootId: %s, eventStreamCount: %d", eventMailBox.getAggregateRootId(), committingContexts.size());
                        }

                        CompletableFuture.runAsync(() ->
                                committingContexts.stream().forEach(context -> publishDomainEventAsync(context.getProcessingCommand(), context.getEventStream()))
                        );

                        eventMailBox.registerForExecution(true);
                    } else if (appendResult == EventAppendResult.DuplicateEvent) {
                        EventCommittingContext context = committingContexts.get(0);
                        if (context.getEventStream().version() == 1) {
                            concatConetxts(committingContexts);
                            handleFirstEventDuplicationAsync(context, 0);
                        } else {
                            _logger.warn("Batch persist event has concurrent version conflict, first eventStream: %s, batchSize: %d", context.getEventStream(), committingContexts.size());
                            resetCommandMailBoxConsumingOffset(context, context.getProcessingCommand().getSequence());
                        }
                    } else if (appendResult == EventAppendResult.DuplicateCommand) {
                        persistEventOneByOne(committingContexts);
                    }
                },
                () -> String.format("[contextListCount:%d]", committingContexts.size()),
                errorMessage ->
                        _logger.fatal(String.format("Batch persist event has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage)),
                retryTimes, true);
    }

    private void persistEventOneByOne(List<EventCommittingContext> contextList) {
        concatConetxts(contextList);
        persistEventAsync(contextList.get(0), 0);
    }

    private void persistEventAsync(EventCommittingContext context, int retryTimes) {
        _ioHelper.tryAsyncActionRecursively("PersistEventAsync",
                () -> _eventStore.appendAsync(context.getEventStream()),
                currentRetryTimes -> persistEventAsync(context, currentRetryTimes),
                result ->
                {
                    if (result.getData() == EventAppendResult.Success) {
                        if (_logger.isDebugEnabled()) {
                            _logger.debug("Persist events success, %s", context.getEventStream());
                        }
                        CompletableFuture.runAsync(() -> publishDomainEventAsync(context.getProcessingCommand(), context.getEventStream()));

                        tryProcessNextContext(context);
                    } else if (result.getData() == EventAppendResult.DuplicateEvent) {
                        //如果是当前事件的版本号为1，则认为是在创建重复的聚合根
                        if (context.getEventStream().version() == 1) {
                            handleFirstEventDuplicationAsync(context, 0);
                        }
                        //如果事件的版本大于1，则认为是更新聚合根时遇到并发冲突了，则需要进行重试；
                        else {
                            _logger.warn("Persist event has concurrent version conflict, eventStream: %s", context.getEventStream());
                            resetCommandMailBoxConsumingOffset(context, context.getProcessingCommand().getSequence());
                        }
                    } else if (result.getData() == EventAppendResult.DuplicateCommand) {
                        _logger.warn("Persist event has duplicate command, eventStream: %s", context.getEventStream());
                        resetCommandMailBoxConsumingOffset(context, context.getProcessingCommand().getSequence() + 1);
                        tryToRepublishEventAsync(context, 0);
                        context.getEventMailBox().registerForExecution(true);
                    }
                },
                () -> String.format("[eventStream:%s]", context.getEventStream()),
                errorMessage -> _logger.fatal(String.format("Persist event has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage)),
                retryTimes, true);
    }

    private void resetCommandMailBoxConsumingOffset(EventCommittingContext context, long consumeOffset) {
        EventMailBox eventMailBox = context.getEventMailBox();
        ProcessingCommand processingCommand = context.getProcessingCommand();
        ProcessingCommandMailbox commandMailBox = processingCommand.getMailbox();

        commandMailBox.stopHandlingMessage();
        updateAggregateMemoryCacheToLatestVersion(context.getEventStream());
        commandMailBox.resetConsumingOffset(consumeOffset);
        eventMailBox.clear();
        eventMailBox.exitHandlingMessage();
//        commandMailBox.restartHandlingMessage();
        commandMailBox.registerForExecution();
    }

    private void tryToRepublishEventAsync(EventCommittingContext context, int retryTimes) {
        ICommand command = context.getProcessingCommand().getMessage();

        _ioHelper.tryAsyncActionRecursively("FindEventByCommandIdAsync",
                () -> _eventStore.findAsync(command.getAggregateRootId(), command.id()),
                currentRetryTimes -> tryToRepublishEventAsync(context, currentRetryTimes),
                result ->
                {
                    DomainEventStream existingEventStream = result.getData();
                    if (existingEventStream != null) {
                        //这里，我们需要再重新做一遍发布事件这个操作；
                        //之所以要这样做是因为虽然该command产生的事件已经持久化成功，但并不表示事件已经发布出去了；
                        //因为有可能事件持久化成功了，但那时正好机器断电了，则发布事件都没有做；
                        publishDomainEventAsync(context.getProcessingCommand(), existingEventStream);
                    } else {
                        //到这里，说明当前command想添加到eventStore中时，提示command重复，但是尝试从eventStore中取出该command时却找不到该command。
                        //出现这种情况，我们就无法再做后续处理了，这种错误理论上不会出现，除非eventStore的Add接口和Get接口出现读写不一致的情况；
                        //框架会记录错误日志，让开发者排查具体是什么问题。
                        String errorMessage = String.format("Command exist in the event store, but we cannot find it from the event store, this should not be happen, and we cannot continue again. commandType:%s, commandId:%s, aggregateRootId:%s",
                                command.getClass().getName(),
                                command.id(),
                                command.getAggregateRootId());
                        _logger.fatal(errorMessage);
                    }
                },
                () -> String.format("[aggregateRootId:%s, commandId:%s]", command.getAggregateRootId(), command.id()),
                errorMessage ->
                {
                    _logger.fatal(String.format("Find event by commandId has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage));
                },
                retryTimes, true);
    }

    private void handleFirstEventDuplicationAsync(EventCommittingContext context, int retryTimes) {
        DomainEventStream eventStream = context.getEventStream();

        _ioHelper.tryAsyncActionRecursively("FindFirstEventByVersion",
                () -> _eventStore.findAsync(eventStream.aggregateRootId(), 1),
                currentRetryTimes -> handleFirstEventDuplicationAsync(context, currentRetryTimes),
                result ->
                {
                    DomainEventStream firstEventStream = result.getData();
                    if (firstEventStream != null) {
                        //判断是否是同一个command，如果是，则再重新做一遍发布事件；
                        //之所以要这样做，是因为虽然该command产生的事件已经持久化成功，但并不表示事件也已经发布出去了；
                        //有可能事件持久化成功了，但那时正好机器断电了，则发布事件都没有做；
                        if (context.getProcessingCommand().getMessage().id().equals(firstEventStream.commandId())) {
                            resetCommandMailBoxConsumingOffset(context, context.getProcessingCommand().getSequence() + 1);
                            publishDomainEventAsync(context.getProcessingCommand(), firstEventStream);
                        } else {
                            //如果不是同一个command，则认为是两个不同的command重复创建ID相同的聚合根，我们需要记录错误日志，然后通知当前command的处理完成；
                            String errorMessage = String.format("Duplicate aggregate creation. current commandId:%s, existing commandId:%s, aggregateRootId:%s, aggregateRootTypeName:%s",
                                    context.getProcessingCommand().getMessage().id(),
                                    firstEventStream.commandId(),
                                    firstEventStream.aggregateRootId(),
                                    firstEventStream.aggregateRootTypeName());
                            _logger.error(errorMessage);
                            resetCommandMailBoxConsumingOffset(context, context.getProcessingCommand().getSequence() + 1);
                            completeCommand(context.getProcessingCommand(), new CommandResult(CommandStatus.Failed, context.getProcessingCommand().getMessage().id(), eventStream.aggregateRootId(), "Duplicate aggregate creation.", String.class.getName()));
                        }
                    } else {
                        String errorMessage = String.format("Duplicate aggregate creation, but we cannot find the existing eventstream from eventstore. commandId:%s, aggregateRootId:%s, aggregateRootTypeName:%s",
                                eventStream.commandId(),
                                eventStream.aggregateRootId(),
                                eventStream.aggregateRootTypeName());
                        _logger.fatal(errorMessage);
                    }
                },
                () -> String.format("[eventStream:%s]", eventStream),
                errorMessage -> _logger.fatal(String.format("Find the first version of event has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage)),
                retryTimes, true);
    }

    private void refreshAggregateMemoryCache(EventCommittingContext context) {
        try {
            context.getAggregateRoot().acceptChanges(context.getEventStream().version());
            _memoryCache.set(context.getAggregateRoot());
        } catch (Exception ex) {
            _logger.error(String.format("Refresh memory cache failed for event stream:%s", context.getEventStream()), ex);
        }
    }

    private void updateAggregateMemoryCacheToLatestVersion(DomainEventStream eventStream) {
        try {
            _memoryCache.refreshAggregateFromEventStore(eventStream.aggregateRootTypeName(), eventStream.aggregateRootId());
        } catch (Exception ex) {
            _logger.error(String.format("Try to refresh aggregate in-memory from event store failed, eventStream: %s", eventStream), ex);
        }
    }

    private void tryProcessNextContext(EventCommittingContext currentContext) {
        if (currentContext.getNext() != null) {
            persistEventAsync(currentContext.getNext(), 0);
        } else {
            currentContext.getEventMailBox().registerForExecution(true);
        }
    }

    private void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStreamMessage eventStream, int retryTimes) {
        _ioHelper.tryAsyncActionRecursively("PublishDomainEventAsync",
                () -> _domainEventPublisher.publishAsync(eventStream),
                currentRetryTimes -> publishDomainEventAsync(processingCommand, eventStream, currentRetryTimes),
                result ->
                {
                    if (_logger.isDebugEnabled()) {
                        _logger.debug("Publish domain events success, %s", eventStream);
                    }
                    String commandHandleResult = processingCommand.getCommandExecuteContext().getResult();
                    completeCommand(processingCommand, new CommandResult(CommandStatus.Success, processingCommand.getMessage().id(), eventStream.aggregateRootId(), commandHandleResult, String.class.getName()));
                },
                () -> String.format("[eventStream:%s]", eventStream),
                errorMessage -> _logger.fatal(String.format("Publish event has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage)),
                retryTimes, true);
    }

    private void concatConetxts(List<EventCommittingContext> contextList) {
        for (int i = 0; i < contextList.size() - 1; i++) {
            EventCommittingContext currentContext = contextList.get(i);
            EventCommittingContext nextContext = contextList.get(i + 1);
            currentContext.setNext(nextContext);
        }
    }

    private void completeCommand(ProcessingCommand processingCommand, CommandResult commandResult) {
        processingCommand.getMailbox().completeMessage(processingCommand, commandResult);
        _logger.info("Complete command, aggregateId: %s", processingCommand.getMessage().getAggregateRootId());
    }
}
