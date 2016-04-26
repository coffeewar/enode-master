package com.qianzhui.enode.common.logging;

/**
 * Created by junbo_xu on 2016/3/31.
 */
public class EmptyLoggerFactory implements ILoggerFactory {
    private static final EmptyLogger Logger = new EmptyLogger();

    public ILogger create(String name) {
        return Logger;
    }

    public ILogger create(Class type) {
        return Logger;
    }
}
