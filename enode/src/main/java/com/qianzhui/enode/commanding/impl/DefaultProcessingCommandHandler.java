package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.commanding.*;
import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.common.io.AsyncTaskStatus;
import com.qianzhui.enode.common.io.IOHelper;
import com.qianzhui.enode.common.io.IORuntimeException;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.serializing.IJsonSerializer;
import com.qianzhui.enode.domain.IAggregateRoot;
import com.qianzhui.enode.domain.IMemoryCache;
import com.qianzhui.enode.eventing.*;
import com.qianzhui.enode.infrastructure.*;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public class DefaultProcessingCommandHandler implements IProcessingCommandHandler {
    private IJsonSerializer _jsonSerializer;
    private ICommandStore _commandStore;
    private IEventStore _eventStore;
    private ICommandHandlerProvider _commandHandlerProvider;
    private ICommandAsyncHandlerProvider _commandAsyncHandlerProvider;
    private ITypeNameProvider _typeNameProvider;
    private IEventService _eventService;
    private IMessagePublisher<IApplicationMessage> _messagePublisher;
    private IMessagePublisher<IPublishableException> _exceptionPublisher;
    private IMemoryCache _memoryCache;
    private IOHelper _ioHelper;
    private ILogger _logger;

    @Inject
    public DefaultProcessingCommandHandler(
            IJsonSerializer jsonSerializer,
            ICommandStore commandStore,
            IEventStore eventStore,
            ICommandHandlerProvider commandHandlerProvider,
            ICommandAsyncHandlerProvider commandAsyncHandlerProvider,
            ITypeNameProvider typeNameProvider,
            IEventService eventService,
            IMessagePublisher<IApplicationMessage> messagePublisher,
            IMessagePublisher<IPublishableException> exceptionPublisher,
            IMemoryCache memoryCache,
            IOHelper ioHelper,
            ILoggerFactory loggerFactory) {
        _jsonSerializer = jsonSerializer;
        _commandStore = commandStore;
        _eventStore = eventStore;
        _commandHandlerProvider = commandHandlerProvider;
        _commandAsyncHandlerProvider = commandAsyncHandlerProvider;
        _typeNameProvider = typeNameProvider;
        _eventService = eventService;
        _messagePublisher = messagePublisher;
        _exceptionPublisher = exceptionPublisher;
        _memoryCache = memoryCache;
        _ioHelper = ioHelper;
        _logger = loggerFactory.create(getClass().getName());
        _eventService.setProcessingCommandHandler(this);
    }

    @Override
    public void handleAsync(ProcessingCommand processingCommand) {
        ICommand command = processingCommand.getMessage();

        if (command.getAggregateRootId() == null || command.getAggregateRootId().trim().equals("")) {
            String errorMessage = String.format("The aggregateRootId of command cannot be null or empty. commandType:%s, commandId:%s", command.getClass().getName(), command.id());
            _logger.error(errorMessage);
            completeCommand(processingCommand, CommandStatus.Failed, String.class.getName(), errorMessage);
            return;
        }

        HandlerFindResult<ICommandHandlerProxy> findResult = getCommandHandler(processingCommand, commandType -> _commandHandlerProvider.getHandlers(commandType));
        if (findResult.getFindStatus() == HandlerFindStatus.Found) {
            handleCommand(processingCommand, findResult.getFindHandler());
        } else if (findResult.getFindStatus() == HandlerFindStatus.TooManyHandlerData) {
            _logger.error("Found more than one command handler data, commandType:%s, commandId:%s", command.getClass().getName(), command.id());
            completeCommand(processingCommand, CommandStatus.Failed, String.class.getName(), "More than one command handler data found.");
        } else if (findResult.getFindStatus() == HandlerFindStatus.TooManyHandler) {
            _logger.error("Found more than one command handler, commandType:%s, commandId:%s", command.getClass().getName(), command.id());
            completeCommand(processingCommand, CommandStatus.Failed, String.class.getName(), "More than one command handler found.");
        } else if (findResult.getFindStatus() == HandlerFindStatus.NotFound) {
            HandlerFindResult<ICommandAsyncHandlerProxy> asyncFindResult = getCommandHandler(processingCommand, commandType -> _commandAsyncHandlerProvider.getHandlers(commandType));
            ICommandAsyncHandlerProxy commandAsyncHandler = asyncFindResult.getFindHandler();
            if (asyncFindResult.getFindStatus() == HandlerFindStatus.Found) {
                handleCommand(processingCommand, commandAsyncHandler);
            } else if (asyncFindResult.getFindStatus() == HandlerFindStatus.TooManyHandlerData) {
                _logger.error("Found more than one command async handler data, commandType:%s, commandId:%s", command.getClass().getName(), command.id());
                completeCommand(processingCommand, CommandStatus.Failed, String.class.getName(), "More than one command async handler data found.");
            } else if (asyncFindResult.getFindStatus() == HandlerFindStatus.TooManyHandler) {
                _logger.error("Found more than one command async handler, commandType:%s, commandId:%s", command.getClass().getName(), command.id());
                completeCommand(processingCommand, CommandStatus.Failed, String.class.getName(), "More than one command async handler found.");
            } else if (asyncFindResult.getFindStatus() == HandlerFindStatus.NotFound) {
                String errorMessage = String.format("No command handler found of command. commandType:%s, commandId:%s", command.getClass().getName(), command.id());
                _logger.error(errorMessage);
                completeCommand(processingCommand, CommandStatus.Failed, String.class.getName(), errorMessage);
            }
        }
    }

    private void handleCommand(ProcessingCommand processingCommand, ICommandHandlerProxy commandHandler) {
        ICommand command = processingCommand.getMessage();

        processingCommand.getCommandExecuteContext().clear();

        //调用command handler执行当前command
        boolean handleSuccess;
        try {
            commandHandler.handle(processingCommand.getCommandExecuteContext(), command);
            if (_logger.isDebugEnabled()) {
                _logger.debug("Handle command success. handlerType:%s, commandType:%s, commandId:%s, aggregateRootId:%s",
                        commandHandler.getInnerObject().getClass().getName(),
                        command.getClass().getName(),
                        command.id(),
                        command.getAggregateRootId());
            }
            handleSuccess = true;
        } catch (Exception ex) {
            handleExceptionAsync(processingCommand, commandHandler, ex, 0);
            return;
        }

        //如果command执行成功，则提交执行后的结果
        if (handleSuccess) {
            try {
                commitAggregateChanges(processingCommand);
            } catch (Exception ex) {
                logCommandExecuteException(processingCommand, commandHandler, ex);
                completeCommand(processingCommand, CommandStatus.Failed, ex.getClass().getName(), "Unknown exception caught when committing changes of command.");
            }
        }
    }

    private void commitAggregateChanges(ProcessingCommand processingCommand) {
        ICommand command = processingCommand.getMessage();
        ICommandExecuteContext context = processingCommand.getCommandExecuteContext();
        List<IAggregateRoot> trackedAggregateRoots = context.getTrackedAggregateRoots();
        int dirtyAggregateRootCount = 0;
        IAggregateRoot dirtyAggregateRoot = null;
        List<IDomainEvent> changedEvents = null;

        for (IAggregateRoot aggregateRoot : trackedAggregateRoots) {
            List<IDomainEvent> events = aggregateRoot.getChanges();

            if (events.size() > 0) {
                dirtyAggregateRootCount++;
                if (dirtyAggregateRootCount > 1) {
                    String errorMessage = String.format("Detected more than one aggregate created or modified by command. commandType:%s, commandId:%s",
                            command.getClass().getName(),
                            command.id());
                    _logger.error(errorMessage);
                    completeCommand(processingCommand, CommandStatus.Failed, String.class.getName(), errorMessage);
                    return;
                }
                dirtyAggregateRoot = aggregateRoot;
                changedEvents = events;
            }
        }
        ;

        //如果当前command没有对任何聚合根做修改，则认为当前command已经处理结束，返回command的结果为NothingChanged
        if (dirtyAggregateRootCount == 0 || changedEvents == null || changedEvents.size() == 0) {
            completeCommand(processingCommand, CommandStatus.NothingChanged, String.class.getName(), context.getResult());
            return;
        }

        //构造出一个事件流对象
        DomainEventStream eventStream = buildDomainEventStream(dirtyAggregateRoot, changedEvents, processingCommand);

        //将事件流提交到EventStore
        _eventService.commitDomainEventAsync(new EventCommittingContext(dirtyAggregateRoot, eventStream, processingCommand));
    }

    private DomainEventStream buildDomainEventStream(IAggregateRoot aggregateRoot, List<IDomainEvent> changedEvents, ProcessingCommand processingCommand) {
        String commandResult = processingCommand.getCommandExecuteContext().getResult();
        if (commandResult != null) {
            processingCommand.getItems().put("CommandResult", commandResult);
        }
        return new DomainEventStream(
                processingCommand.getMessage().id(),
                aggregateRoot.uniqueId(),
                _typeNameProvider.getTypeName(aggregateRoot.getClass()),
                aggregateRoot.version() + 1,
                new Date(),
                new ArrayList<>(changedEvents),
                processingCommand.getItems());
    }

    private void handleExceptionAsync(ProcessingCommand processingCommand, ICommandHandlerProxy commandHandler, Exception exception, int retryTimes) {
        ICommand command = processingCommand.getMessage();

        _ioHelper.tryAsyncActionRecursively("FindEventByCommandIdAsync",
                () -> _eventStore.findAsync(command.getAggregateRootId(), command.id()),
                (currentRetryTimes) -> handleExceptionAsync(processingCommand, commandHandler, exception, currentRetryTimes),
                result -> {
                    DomainEventStream existingEventStream = result.getData();

                    if (existingEventStream != null) {
                        //这里，我们需要再重新做一遍更新内存缓存以及发布事件这两个操作；
                        //之所以要这样做是因为虽然该command产生的事件已经持久化成功，但并不表示已经内存也更新了或者事件已经发布出去了；
                        //因为有可能事件持久化成功了，但那时正好机器断电了，则更新内存和发布事件都没有做；
                        //_memoryCache.refreshAggregateFromEventStore(existingEventStream.aggregateRootTypeName(), existingEventStream.aggregateRootId());
                        _eventService.publishDomainEventAsync(processingCommand, existingEventStream);
                    } else {
                        //到这里，说明当前command执行遇到异常，然后当前command之前也没执行过，是第一次被执行。
                        //那就判断当前异常是否是需要被发布出去的异常，如果是，则发布该异常给所有消费者；否则，就记录错误日志；
                        //然后，认为该command处理失败即可；

                        Exception exp = exception;
                        if(exp instanceof WrappedRuntimeException)
                            exp = ((WrappedRuntimeException)exp).getException();

                        if(exp instanceof IPublishableException) {
                            IPublishableException publishableException = (IPublishableException) exp;
                            publishExceptionAsync(processingCommand, publishableException, 0);
                        } else {
                            logCommandExecuteException(processingCommand, commandHandler, exp);
                            completeCommand(processingCommand, CommandStatus.Failed, exp.getClass().getName(), exp.getMessage());
                        }
                    }
                },
                () -> String.format("[commandId:%s]", command.id()),
                errorMessage -> _logger.fatal(String.format("Find event by commandId has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage)),
                retryTimes, true, 3, 1000
        );

    }

    private void publishExceptionAsync(ProcessingCommand processingCommand, IPublishableException exception, int retryTimes) {
        _ioHelper.tryAsyncActionRecursively("PublishExceptionAsync",
                () -> _exceptionPublisher.publishAsync(exception),
                currentRetryTimes -> publishExceptionAsync(processingCommand, exception, currentRetryTimes),
                result ->

                        completeCommand(processingCommand, CommandStatus.Failed, exception.getClass().getName(), ((Exception) exception).getMessage())
                ,
                () ->
                {
                    Map<String, String> serializableInfo = new HashMap<>();
                    exception.serializeTo(serializableInfo);
                    String exceptionInfo = String.join(",", serializableInfo.entrySet().stream().map(x -> String.format("%s:%s", x.getKey(), x.getValue())).collect(Collectors.toList()));
                    return String.format("[commandId:%s, exceptionInfo:%s]", processingCommand.getMessage().id(), exceptionInfo);
                },
                errorMessage -> _logger.fatal(String.format("Publish event has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage)),
                retryTimes, true, 3, 1000);
    }

    /*private void notifyCommandExecuted(ProcessingCommand processingCommand, CommandStatus commandStatus, String resultType, String result) {
        processingCommand.complete(new CommandResult(commandStatus, processingCommand.getMessage().id(), processingCommand.getMessage().getAggregateRootId(), result, resultType));
    }

    private void retryCommand(ProcessingCommand processingCommand) {
        processingCommand.getCommandExecuteContext().clear();
        handleAsync(processingCommand);
    }*/

    private void logCommandExecuteException(ProcessingCommand processingCommand, ICommandHandlerProxy commandHandler, Exception exception) {
        ICommand command = processingCommand.getMessage();
        String errorMessage = String.format("%s raised when %s handling %s. commandId:%s, aggregateRootId:%s",
                exception.getClass().getName(),
                commandHandler.getInnerObject().getClass().getName(),
                command.getClass().getName(),
                command.id(),
                command.getAggregateRootId());
        _logger.error(errorMessage, exception);
    }

    /*private ICommandAsyncHandlerProxy getCommandAsyncHandler(ProcessingCommand processingCommand) {
        ICommand command = processingCommand.getMessage();
        List<MessageHandlerData<ICommandAsyncHandlerProxy>> commandAsyncHandlers = _commandAsyncHandlerProvider.getHandlers(command.getClass());

        if (commandAsyncHandlers.size() > 1) {
            _logger.error("Found more than one command handlers, commandType:%s, commandId:%s.", command.getClass().getName(), command.id());
            notifyCommandExecuted(processingCommand, CommandStatus.Failed, String.class.getName(), "More than one command handlers found.");
            return null;
        }

        MessageHandlerData<ICommandAsyncHandlerProxy> handlerData = commandAsyncHandlers.get(0);
        if (handlerData != null) {
            return handlerData.ListHandlers.get(0);
        }
        return null;
    }*/

    private void handleCommand(ProcessingCommand processingCommand, ICommandAsyncHandlerProxy commandHandler) {
        if (commandHandler.checkCommandHandledFirst()) {
            processCommand(processingCommand, commandHandler, 0);
        } else {
            handleCommandAsync(processingCommand, commandHandler, 0);
        }
    }

    private void processCommand(ProcessingCommand processingCommand, ICommandAsyncHandlerProxy commandAsyncHandler, int retryTimes) {
        ICommand command = processingCommand.getMessage();

        _ioHelper.tryAsyncActionRecursively("GetCommandAsync",
                () -> _commandStore.getAsync(command.id()),
                currentRetryTimes -> processCommand(processingCommand, commandAsyncHandler, currentRetryTimes),
                result ->
                {
                    HandledCommand existingHandledCommand = result.getData();
                    if (existingHandledCommand != null) {
                        if (existingHandledCommand.getMessage() != null) {
                            publishMessageAsync(processingCommand, existingHandledCommand.getMessage(), 0);
                        } else {
                            completeCommand(processingCommand, CommandStatus.Success, null, null);
                        }
                        return;
                    }

                    handleCommandAsync(processingCommand, commandAsyncHandler, 0);
                },
                () -> String.format("[commandId:%s,commandType:%s]", command.id(), command.getClass().getName()),
                errorMessage -> _logger.fatal(String.format("Get command by id has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage)),
                retryTimes, true, 3, 1000);
    }

    private void handleCommandAsync(ProcessingCommand processingCommand, ICommandAsyncHandlerProxy commandHandler, int retryTimes) {
        ICommand command = processingCommand.getMessage();

        _ioHelper.tryAsyncActionRecursively("HandleCommandAsync",
                () ->
                {
                    try {
                        CompletableFuture<AsyncTaskResult<IApplicationMessage>> asyncResult = commandHandler.handleAsync(command);
                        if (_logger.isDebugEnabled()) {
                            _logger.debug("Handle command async success. handlerType:%s, commandType:%s, commandId:%s, aggregateRootId:%s",
                                    commandHandler.getInnerObject().getClass().getName(),
                                    command.getClass().getName(),
                                    command.id(),
                                    command.getAggregateRootId());
                        }
                        return asyncResult;
                    } catch (IORuntimeException ex) {
                        _logger.error(String.format("Handle command async has io exception. handlerType:%s, commandType:%s, commandId:%s, aggregateRootId:%s",
                                commandHandler.getInnerObject().getClass().getName(),
                                command.getClass().getName(),
                                command.id(),
                                command.getAggregateRootId()), ex);
                        return CompletableFuture.completedFuture(new AsyncTaskResult<>(AsyncTaskStatus.IOException, ex.getMessage()));
                    } catch (Exception ex) {
                        _logger.error(String.format("Handle command async has unknown exception. handlerType:%s, commandType:%s, commandId:%s, aggregateRootId:%s",
                                commandHandler.getInnerObject().getClass().getName(),
                                command.getClass().getName(),
                                command.id(),
                                command.getAggregateRootId()), ex);
                        return CompletableFuture.completedFuture(new AsyncTaskResult<>(AsyncTaskStatus.Failed, ex.getMessage()));
                    }
                },
                currentRetryTimes -> handleCommandAsync(processingCommand, commandHandler, currentRetryTimes),
                result ->
                        commitChangesAsync(processingCommand, true, result.getData(), null, 0),
                () -> String.format("[command:[id:%s,type:%s],handlerType:%s]", command.id(), command.getClass().getName(), commandHandler.getInnerObject().getClass().getName()),
                errorMessage -> commitChangesAsync(processingCommand, false, null, errorMessage, 0),
                retryTimes);
    }

    private void commitChangesAsync(ProcessingCommand processingCommand, boolean success, IApplicationMessage message, String errorMessage, int retryTimes) {
        ICommand command = processingCommand.getMessage();
        HandledCommand handledCommand = new HandledCommand(command.id(), command.getAggregateRootId(), message);

        _ioHelper.tryAsyncActionRecursively("AddCommandAsync",
                () -> _commandStore.addAsync(handledCommand),
                currentRetryTimes -> commitChangesAsync(processingCommand, success, message, errorMessage, currentRetryTimes),
                result ->
                {
                    CommandAddResult commandAddResult = result.getData();
                    if (commandAddResult == CommandAddResult.Success) {
                        if (success) {
                            if (message != null) {
                                publishMessageAsync(processingCommand, message, 0);
                            } else {
                                completeCommand(processingCommand, CommandStatus.Success, null, null);
                            }
                        } else {
                            completeCommand(processingCommand, CommandStatus.Failed, String.class.getName(), errorMessage);
                        }
                    } else if (commandAddResult == CommandAddResult.DuplicateCommand) {
                        handleDuplicatedCommandAsync(processingCommand, 0);
                    }
                },
                () -> String.format("[handledCommand:%s]", handledCommand),
                error -> _logger.fatal(String.format("Add command has unknown exception, the code should not be run to here, errorMessage: %s", error)),
                retryTimes, true, 3, 1000);
    }

    private void publishMessageAsync(ProcessingCommand processingCommand, IApplicationMessage message, int retryTimes) {
        ICommand command = processingCommand.getMessage();

        _ioHelper.tryAsyncActionRecursively("PublishApplicationMessageAsync",
                () -> _messagePublisher.publishAsync(message),
                currentRetryTimes -> publishMessageAsync(processingCommand, message, currentRetryTimes),
                result -> completeCommand(processingCommand, CommandStatus.Success, message.getTypeName(), _jsonSerializer.serialize(message)),
                () -> String.format("[application message:[id:%s,type:%s],command:[id:%s,type:%s]]", message.id(), message.getClass().getName(), command.id(), command.getClass().getName()),
                errorMessage -> _logger.fatal(String.format("Publish application message has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage)),
                retryTimes, true, 3, 1000);
    }

    private void handleDuplicatedCommandAsync(ProcessingCommand processingCommand, int retryTimes) {
        ICommand command = processingCommand.getMessage();

        _ioHelper.tryAsyncActionRecursively("GetCommandAsync",
                () -> _commandStore.getAsync(command.id()),
                currentRetryTimes -> handleDuplicatedCommandAsync(processingCommand, currentRetryTimes),
                result ->
                {
                    HandledCommand existingHandledCommand = result.getData();
                    if (existingHandledCommand != null) {
                        if (existingHandledCommand.getMessage() != null) {
                            publishMessageAsync(processingCommand, existingHandledCommand.getMessage(), 0);
                        } else {
                            completeCommand(processingCommand, CommandStatus.Success, null, null);
                        }
                    } else {
                        //到这里，说明当前command想添加到commandStore中时，提示command重复，但是尝试从commandStore中取出该command时却找不到该command。
                        //出现这种情况，我们就无法再做后续处理了，这种错误理论上不会出现，除非commandStore的Add接口和Get接口出现读写不一致的情况；
                        //我们记录错误日志，然后认为当前command已被处理为失败。
                        String errorMessage = String.format("Command exist in the command store, but we cannot get it from the command store. commandType:%s, commandId:%s, aggregateRootId:%s",
                                command.getClass().getName(),
                                command.id(),
                                command.getAggregateRootId());
                        _logger.error(errorMessage);
                        completeCommand(processingCommand, CommandStatus.Failed, null, errorMessage);
                    }
                },
                () -> String.format("[command:[id:%s,type:%s]", command.id(), command.getClass().getName()),
                errorMessage -> _logger.fatal(String.format("Get command by id has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage)),
                retryTimes, true, 3, 1000);
    }

    private <T extends IObjectProxy> HandlerFindResult<T> getCommandHandler(ProcessingCommand processingCommand, Function<Class, List<MessageHandlerData<T>>> getHandlersFunc) {
        ICommand command = processingCommand.getMessage();
        List<MessageHandlerData<T>> handlerDataList = getHandlersFunc.apply(command.getClass());

        if (handlerDataList == null || handlerDataList.size() == 0) {
            return HandlerFindResult.NotFound;
        } else if (handlerDataList.size() > 1) {
            return HandlerFindResult.TooManyHandlerData;
        }

        MessageHandlerData<T> handlerData = handlerDataList.get(0);

        if (handlerData.ListHandlers == null || handlerData.ListHandlers.size() == 0) {
            return HandlerFindResult.NotFound;
        } else if (handlerData.ListHandlers.size() > 1) {
            return HandlerFindResult.TooManyHandler;
        }

        T handlerProxy = handlerData.ListHandlers.get(0);

        return new HandlerFindResult<>(HandlerFindStatus.Found, handlerProxy);
    }

    static class HandlerFindResult<T extends IObjectProxy> {

        static HandlerFindResult NotFound = new HandlerFindResult<>(HandlerFindStatus.NotFound);
        static HandlerFindResult TooManyHandlerData = new HandlerFindResult<>(HandlerFindStatus.TooManyHandlerData);
        static HandlerFindResult TooManyHandler = new HandlerFindResult<>(HandlerFindStatus.TooManyHandler);

        private HandlerFindStatus findStatus;
        private T findHandler;

        HandlerFindResult(HandlerFindStatus findStatus) {
            this(findStatus, null);
        }

        public HandlerFindResult(HandlerFindStatus findStatus, T findHandler) {
            this.findStatus = findStatus;
            this.findHandler = findHandler;
        }

        public HandlerFindStatus getFindStatus() {
            return findStatus;
        }

        public void setFindStatus(HandlerFindStatus findStatus) {
            this.findStatus = findStatus;
        }

        public T getFindHandler() {
            return findHandler;
        }

        public void setFindHandler(T findHandler) {
            this.findHandler = findHandler;
        }
    }

    private void completeCommand(ProcessingCommand processingCommand, CommandStatus commandStatus, String resultType, String result) {
        CommandResult commandResult = new CommandResult(commandStatus, processingCommand.getMessage().id(), processingCommand.getMessage().getAggregateRootId(), result, resultType);
        processingCommand.getMailbox().completeMessage(processingCommand, commandResult);
        processingCommand.getMailbox().tryExecuteNextMessage();
    }

    enum HandlerFindStatus {
        NotFound,
        Found,
        TooManyHandlerData,
        TooManyHandler
    }
}
