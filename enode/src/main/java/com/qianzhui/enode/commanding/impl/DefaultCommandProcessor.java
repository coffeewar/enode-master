package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.commanding.*;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by junbo_xu on 2016/4/22.
 */
public class DefaultCommandProcessor implements ICommandProcessor {
    private final ILogger _logger;
    private final ConcurrentMap<String, ProcessingCommandMailbox> _mailboxDict;
    private final IProcessingCommandScheduler _scheduler;
    private final IProcessingCommandHandler _handler;

    @Inject
    public DefaultCommandProcessor(IProcessingCommandScheduler scheduler, IProcessingCommandHandler handler, ILoggerFactory loggerFactory)
    {
        _mailboxDict = new ConcurrentHashMap<>();
        _scheduler = scheduler;
        _handler = handler;
        _logger = loggerFactory.create(getClass());
    }

    public void process(ProcessingCommand processingCommand)
    {
        String aggregateRootId = processingCommand.getMessage().getAggregateRootId();
        if (aggregateRootId == null || aggregateRootId.trim().equals(""))
        {
            throw new IllegalArgumentException("aggregateRootId of command cannot be null or empty, commandId:" + processingCommand.getMessage().id());
        }

        ProcessingCommandMailbox mailbox = _mailboxDict.computeIfAbsent(aggregateRootId, x -> new ProcessingCommandMailbox(x, _scheduler, _handler, _logger));
        mailbox.enqueueMessage(processingCommand);
    }
}
