package com.qianzhui.enode.common.logging;

/**
 * Created by junbo_xu on 2016/3/1.
 */
public interface ILogger {
    boolean isDebugEnabled();

    void debug(Object message);

    void debug(String format, Object... args);

    void debug(Object message, Exception exception);

    void debug(Object message, Throwable exception);

    void info(Object message);

    void info(String format, Object... args);

    void info(Object message, Exception exception);

    void info(Object message, Throwable exception);

    void error(Object message);

    void error(String format, Object... args);

    void error(Object message, Exception exception);

    void error(Object message, Throwable exception);

    void warn(Object message);

    void warn(String format, Object... args);

    void warn(Object message, Exception exception);

    void warn(Object message, Throwable exception);

    void fatal(Object message);

    void fatal(String format, Object... args);

    void fatal(Object message, Exception exception);

    void fatal(Object message, Throwable exception);
}
