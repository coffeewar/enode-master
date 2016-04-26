package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.commanding.CommandResult;
import com.qianzhui.enode.commanding.CommandReturnType;
import com.qianzhui.enode.commanding.ICommand;
import com.qianzhui.enode.commanding.ICommandService;
import com.qianzhui.enode.common.io.AsyncTaskResult;

import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/2/27.
 */
public class NotImplementedCommandService implements ICommandService {
    @Override
    public CompletableFuture<AsyncTaskResult> sendAsync(ICommand command) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(ICommand command) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CommandResult execute(ICommand command, int timeoutMillis) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CommandResult execute(ICommand command, CommandReturnType commandReturnType, int timeoutMillis) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<AsyncTaskResult<CommandResult>> executeAsync(ICommand command) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<AsyncTaskResult<CommandResult>> executeAsync(ICommand command, CommandReturnType commandReturnType) {
        throw new UnsupportedOperationException();
    }
}
