package com.qianzhui.enode.infrastructure;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public interface IApplicationMessage extends IMessage {
    long getStartDeliverTime();
    void setStartDeliverTime(long startDeliverTime);
}
