package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.infrastructure.*;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public class DefaultMessageProcessor<X extends IProcessingMessage<X, Y, Z>, Y extends IMessage, Z> implements IMessageProcessor<X, Y, Z> {

    private ConcurrentMap<String, ProcessingMessageMailbox<X, Y, Z>> _mailboxDict;
    private IProcessingMessageScheduler<X, Y, Z> _processingMessageScheduler;
    private IProcessingMessageHandler<X, Y, Z> _processingMessageHandler;

    @Inject
    public DefaultMessageProcessor(IProcessingMessageScheduler<X, Y, Z> processingMessageScheduler, IProcessingMessageHandler<X, Y, Z> processingMessageHandler) {
        _mailboxDict = new ConcurrentHashMap<>();
        _processingMessageScheduler = processingMessageScheduler;
        _processingMessageHandler = processingMessageHandler;
    }

    @Override
    public void process(X processingMessage) {
        String routingKey = processingMessage.getMessage().getRoutingKey();
        if (routingKey != null && !routingKey.trim().equals("")) {
//            ProcessingMessageMailbox<X, Y, Z> mailbox = _mailboxDict.putIfAbsent(routingKey, new ProcessingMessageMailbox<>(_processingMessageScheduler, _processingMessageHandler));
            ProcessingMessageMailbox<X, Y, Z> mailbox = _mailboxDict.computeIfAbsent(routingKey, key -> new ProcessingMessageMailbox<>(_processingMessageScheduler, _processingMessageHandler));
            mailbox.enqueueMessage(processingMessage);
            _processingMessageScheduler.scheduleMailbox(mailbox);
        } else {
            _processingMessageScheduler.scheduleMessage(processingMessage);
        }
    }
}
