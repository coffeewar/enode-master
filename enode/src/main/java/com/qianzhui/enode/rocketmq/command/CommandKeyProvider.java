package com.qianzhui.enode.rocketmq.command;

import com.qianzhui.enode.commanding.ICommand;
import com.qianzhui.enode.commanding.ICommandKeyProvider;

/**
 * Created by junbo_xu on 2016/3/9.
 */
public class CommandKeyProvider implements ICommandKeyProvider {
    @Override
    public String getKey(ICommand command) {
//        return command.getAggregateRootId() == null ? command.id() : command.id() + MessageConst.KEY_SEPARATOR + command.getAggregateRootId();
        return command.getAggregateRootId() == null ? command.id() : command.getAggregateRootId();
    }
}
