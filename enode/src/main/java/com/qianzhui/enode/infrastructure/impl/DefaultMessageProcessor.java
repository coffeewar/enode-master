package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.infrastructure.*;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public class DefaultMessageProcessor<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements IMessageProcessor<X, Y> {

    private final ILogger _logger;
    private ConcurrentMap<String, ProcessingMessageMailbox<X, Y>> _mailboxDict;
    private IProcessingMessageScheduler<X, Y> _processingMessageScheduler;
    private IProcessingMessageHandler<X, Y> _processingMessageHandler;

    @Inject
    public DefaultMessageProcessor(IProcessingMessageScheduler<X, Y> processingMessageScheduler, IProcessingMessageHandler<X, Y> processingMessageHandler, ILoggerFactory loggerFactory) {
        _mailboxDict = new ConcurrentHashMap<>();
        _processingMessageScheduler = processingMessageScheduler;
        _processingMessageHandler = processingMessageHandler;
        _logger = loggerFactory.create(getClass());
    }

    @Override
    public void process(X processingMessage) {
        String routingKey = processingMessage.getMessage().getRoutingKey();
        if (routingKey != null && !routingKey.trim().equals("")) {
//            ProcessingMessageMailbox<X, Y, Z> mailbox = _mailboxDict.putIfAbsent(routingKey, new ProcessingMessageMailbox<>(_processingMessageScheduler, _processingMessageHandler));
            ProcessingMessageMailbox<X, Y> mailbox = _mailboxDict.computeIfAbsent(routingKey, key -> new ProcessingMessageMailbox<>(routingKey, _processingMessageScheduler, _processingMessageHandler, _logger));
            mailbox.enqueueMessage(processingMessage);
        } else {
            _processingMessageScheduler.scheduleMessage(processingMessage);
        }
    }
}
