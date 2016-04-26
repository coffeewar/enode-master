package com.qianzhui.enode.commanding;

import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.infrastructure.IApplicationMessage;
import com.qianzhui.enode.infrastructure.IObjectProxy;

import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public interface ICommandAsyncHandlerProxy extends IObjectProxy {
    CompletableFuture<AsyncTaskResult<IApplicationMessage>> handleAsync(ICommand command);

    boolean checkCommandHandledFirst();
}
