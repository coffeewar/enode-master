package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.infrastructure.IMessage;
import com.qianzhui.enode.infrastructure.IMessageDispatcher;
import com.qianzhui.enode.infrastructure.IProcessingMessage;
import com.qianzhui.enode.infrastructure.IProcessingMessageHandler;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/4/5.
 */
public class DefaultProcessingMessageHandler<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements IProcessingMessageHandler<X, Y> {
    private final IMessageDispatcher _dispatcher;

    @Inject
    public DefaultProcessingMessageHandler(IMessageDispatcher dispatcher) {
        _dispatcher = dispatcher;
    }

    public void handleAsync(X processingMessage) {
        CompletableFuture<AsyncTaskResult> asyncTaskResultCompletableFuture = _dispatcher.dispatchMessageAsync(processingMessage.getMessage());
        asyncTaskResultCompletableFuture.thenRun(() ->
                processingMessage.complete()
        );
    }
}
