package com.qianzhui.enode.commanding;

import com.qianzhui.enode.infrastructure.Message;

/**
 * Created by junbo_xu on 2016/3/24.
 */
public class Command<TAggregateRootId> extends Message implements ICommand {
    public TAggregateRootId aggregateRootId;

    public Command() {
        super();
    }

    public Command(TAggregateRootId aggregateRootId) {
        super();
        if (aggregateRootId == null) {
            throw new NullPointerException("aggregateRootId");
        }
        this.aggregateRootId = aggregateRootId;
    }

    @Override
    public String getAggregateRootId() {
        if (this.aggregateRootId != null)
            return this.aggregateRootId.toString();
        else
            return null;
    }

    @Override
    public String getRoutingKey() {
        return getAggregateRootId();
    }
}
