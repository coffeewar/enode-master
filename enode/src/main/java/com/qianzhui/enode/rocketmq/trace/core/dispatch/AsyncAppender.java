package com.qianzhui.enode.rocketmq.trace.core.dispatch;

/**
 * Created by junbo_xu on 2016/10/17.
 */
public abstract class AsyncAppender {
    public AsyncAppender() {
    }

    public abstract void append(Object var1);

    public abstract void flush();
}
