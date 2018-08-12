package com.qianzhui.enode.infrastructure;

/**
 * Created by junbo_xu on 2016/4/6.
 */
public abstract class ApplicationMessage extends Message implements IApplicationMessage {
    private long startDeliverTime;
    @Override
    public long getStartDeliverTime() {
        return startDeliverTime;
    }

    @Override
    public void setStartDeliverTime(long startDeliverTime) {
        this.startDeliverTime = startDeliverTime;
    }
}
