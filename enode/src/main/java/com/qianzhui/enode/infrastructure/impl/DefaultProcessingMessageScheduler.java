package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.infrastructure.*;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public class DefaultProcessingMessageScheduler<X extends IProcessingMessage<X, Y, Z>, Y extends IMessage, Z> implements IProcessingMessageScheduler<X, Y, Z> {
    private IProcessingMessageHandler<X, Y, Z> _messageHandler;

    @Inject
    public DefaultProcessingMessageScheduler(IProcessingMessageHandler<X, Y, Z> messageHandler) {
        _messageHandler = messageHandler;
    }

    @Override
    public void scheduleMessage(X processingMessage) {
        CompletableFuture.runAsync(() -> _messageHandler.handleAsync(processingMessage));
    }

    @Override
    public void scheduleMailbox(ProcessingMessageMailbox<X, Y, Z> mailbox) {
        if (mailbox.enterHandlingMessage()) {
            CompletableFuture.runAsync(mailbox::run);
        }
    }
}
