package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.infrastructure.IMessage;
import com.qianzhui.enode.infrastructure.IMessageDispatcher;
import com.qianzhui.enode.infrastructure.IProcessingMessage;
import com.qianzhui.enode.infrastructure.IProcessingMessageHandler;

import javax.inject.Inject;

/**
 * Created by junbo_xu on 2016/4/5.
 */
public class DefaultProcessingMessageHandler<X extends IProcessingMessage<X, Y, Z>, Y extends IMessage, Z> implements IProcessingMessageHandler<X, Y, Z> {
    private final IMessageDispatcher _dispatcher;

    @Inject
    public DefaultProcessingMessageHandler(IMessageDispatcher dispatcher) {
        _dispatcher = dispatcher;
    }

    public void handleAsync(X processingMessage) {
        _dispatcher.dispatchMessageAsync(processingMessage.getMessage());
        //TODO default(Z)
        //processingMessage.Complete(default(Z));
        processingMessage.setResult(null);
    }
}
