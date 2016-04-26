package com.qianzhui.enode.common.logging;

/**
 * Created by junbo_xu on 2016/3/1.
 */
public interface ILoggerFactory {
    ILogger create(String name);

    ILogger create(Class type);
}
