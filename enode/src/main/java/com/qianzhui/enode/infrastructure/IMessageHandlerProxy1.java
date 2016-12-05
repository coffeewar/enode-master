package com.qianzhui.enode.infrastructure;

import com.qianzhui.enode.common.io.AsyncTaskResult;

import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/3/31.
 */
public interface IMessageHandlerProxy1 extends IObjectProxy, MethodInvocation {
    CompletableFuture<AsyncTaskResult> handleAsync(IMessage message);
}
