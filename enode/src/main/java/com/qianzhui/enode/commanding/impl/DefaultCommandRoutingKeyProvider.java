package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.commanding.ICommand;
import com.qianzhui.enode.commanding.ICommandRoutingKeyProvider;

/**
 * Created by junbo_xu on 2016/3/2.
 */
public class DefaultCommandRoutingKeyProvider implements ICommandRoutingKeyProvider {
    @Override
    public String getRoutingKey(ICommand command) {
        if (!(command.getAggregateRootId() == null || command.getAggregateRootId().trim().equals("")))
            return command.getAggregateRootId();

        return command.id();
    }
}
