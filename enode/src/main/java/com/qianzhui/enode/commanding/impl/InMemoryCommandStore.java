package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.commanding.CommandAddResult;
import com.qianzhui.enode.commanding.HandledCommand;
import com.qianzhui.enode.commanding.ICommandStore;
import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.common.io.AsyncTaskStatus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by junbo_xu on 2016/3/20.
 */
public class InMemoryCommandStore implements ICommandStore {

    private ConcurrentMap<String, HandledCommand> _handledCommandDict = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<AsyncTaskResult<CommandAddResult>> addAsync(HandledCommand handledCommand) {
        return CompletableFuture.completedFuture(new AsyncTaskResult<CommandAddResult>(AsyncTaskStatus.Success, null, add(handledCommand)));
    }

    @Override
    public CompletableFuture<AsyncTaskResult<HandledCommand>> getAsync(String commandId) {
        return CompletableFuture.completedFuture(new AsyncTaskResult<HandledCommand>(AsyncTaskStatus.Success, null, get(commandId)));
    }

    private CommandAddResult add(HandledCommand handledCommand) {
        if (_handledCommandDict.containsKey(handledCommand.getCommandId())) {
            return CommandAddResult.DuplicateCommand;
        }

        _handledCommandDict.putIfAbsent(handledCommand.getCommandId(), handledCommand);

        return CommandAddResult.Success;
    }

    private HandledCommand get(String commandId) {
        return _handledCommandDict.get(commandId);
    }
}
