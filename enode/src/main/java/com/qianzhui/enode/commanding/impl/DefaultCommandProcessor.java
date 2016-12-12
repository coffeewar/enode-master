package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.ENode;
import com.qianzhui.enode.commanding.ICommandProcessor;
import com.qianzhui.enode.commanding.IProcessingCommandHandler;
import com.qianzhui.enode.commanding.ProcessingCommand;
import com.qianzhui.enode.commanding.ProcessingCommandMailbox;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.scheduling.IScheduleService;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by junbo_xu on 2016/4/22.
 */
public class DefaultCommandProcessor implements ICommandProcessor {
    private final ILogger _logger;
    private final ConcurrentMap<String, ProcessingCommandMailbox> _mailboxDict;
    private final IProcessingCommandHandler _handler;
    private final IScheduleService _scheduleService;
    private final int _timeoutSeconds;
    private final String _taskName;

    @Inject
    public DefaultCommandProcessor(IScheduleService scheduleService, IProcessingCommandHandler handler, ILoggerFactory loggerFactory) {
        _scheduleService = scheduleService;
        _mailboxDict = new ConcurrentHashMap<>();
        _handler = handler;
        _logger = loggerFactory.create(getClass());
        _timeoutSeconds = ENode.getInstance().getSetting().getAggregateRootMaxInactiveSeconds();
        _taskName = "CleanInactiveAggregates_" + System.nanoTime() + new Random().nextInt(10000);
    }

    public void process(ProcessingCommand processingCommand) {
        String aggregateRootId = processingCommand.getMessage().getAggregateRootId();
        if (aggregateRootId == null || aggregateRootId.trim().equals("")) {
            throw new IllegalArgumentException("aggregateRootId of command cannot be null or empty, commandId:" + processingCommand.getMessage().id());
        }

        ProcessingCommandMailbox mailbox = _mailboxDict.computeIfAbsent(aggregateRootId, x -> new ProcessingCommandMailbox(x, _handler, _logger));
        mailbox.enqueueMessage(processingCommand);
    }

    public void start() {
        _scheduleService.startTask(_taskName, this::cleanInactiveMailbox,
                ENode.getInstance().getSetting().getScanExpiredAggregateIntervalMilliseconds(),
                ENode.getInstance().getSetting().getScanExpiredAggregateIntervalMilliseconds());
    }

    public void stop() {
        _scheduleService.stopTask(_taskName);
    }

    private void cleanInactiveMailbox() {
        List<Map.Entry<String, ProcessingCommandMailbox>> inactiveList = _mailboxDict.entrySet().stream().filter(entry ->
                entry.getValue().isInactive(_timeoutSeconds) && !entry.getValue().isRunning()
        ).collect(Collectors.toList());

        inactiveList.stream().forEach(entry -> {
            if (_mailboxDict.remove(entry.getKey()) != null) {
                _logger.info("Removed inactive command mailbox, aggregateRootId: %s", entry.getKey());
            }
        });
    }
}
