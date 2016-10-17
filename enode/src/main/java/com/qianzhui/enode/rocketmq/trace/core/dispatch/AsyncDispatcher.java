package com.qianzhui.enode.rocketmq.trace.core.dispatch;

/**
 * Created by junbo_xu on 2016/10/17.
 */
import java.io.IOException;

public abstract class AsyncDispatcher {
    public AsyncDispatcher() {
    }

    public abstract void start(AsyncAppender var1, String var2);

    public abstract boolean append(Object var1);

    public abstract void flush() throws IOException;

    public abstract void registerShutDownHook();
}