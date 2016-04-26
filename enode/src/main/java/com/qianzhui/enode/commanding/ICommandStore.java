package com.qianzhui.enode.commanding;

import com.qianzhui.enode.common.io.AsyncTaskResult;

import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public interface ICommandStore {
    CompletableFuture<AsyncTaskResult<CommandAddResult>> addAsync(HandledCommand handledCommand);

    CompletableFuture<AsyncTaskResult<HandledCommand>> getAsync(String commandId);
}
