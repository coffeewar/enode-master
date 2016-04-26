package com.qianzhui.enode.infrastructure;

import com.qianzhui.enode.common.io.AsyncTaskResult;

import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/4/3.
 */
public interface IMessageHandlerProxy2 extends IObjectProxy {
    CompletableFuture<AsyncTaskResult> handleAsync(IMessage message1, IMessage message2);
}
