package com.qianzhui.enode.commanding;

import com.qianzhui.enode.infrastructure.IMessage;

/**
 * Created by junbo_xu on 2016/2/27.
 * Represents a command.
 */
public interface ICommand extends IMessage {
    /**
     * Represents the associated aggregate root string id.
     */
    String getAggregateRootId();
    long getStartDeliverTime();
    void setStartDeliverTime(long startDeliverTime);
}
