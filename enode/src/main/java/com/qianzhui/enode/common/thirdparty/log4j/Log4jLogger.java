package com.qianzhui.enode.common.thirdparty.log4j;

import com.qianzhui.enode.common.logging.ILogger;
import org.apache.log4j.Logger;

/**
 * Created by junbo_xu on 2016/3/1.
 */
public class Log4jLogger implements ILogger {

    private Logger log;

    public Log4jLogger(Logger log) {
        this.log = log;
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(Object message) {
        log.debug(message);
    }

    @Override
    public void debug(String format, Object... args) {
        log.debug(format(format, args));
    }

    @Override
    public void debug(Object message, Exception exception) {
        log.debug(message, exception);
    }

    @Override
    public void info(Object message) {
        log.info(message);
    }

    @Override
    public void info(String format, Object... args) {
        log.info(format(format, args));
    }

    @Override
    public void info(Object message, Exception exception) {
        log.info(message, exception);
    }

    @Override
    public void error(Object message) {
        log.error(message);
    }

    @Override
    public void error(String format, Object... args) {
        log.error(format(format, args));
    }

    @Override
    public void error(Object message, Exception exception) {
        log.error(message, exception);
    }

    @Override
    public void warn(Object message) {
        log.warn(message);
    }

    @Override
    public void warn(String format, Object... args) {
        log.warn(format(format, args));
    }

    @Override
    public void warn(Object message, Exception exception) {
        log.warn(message, exception);
    }

    @Override
    public void fatal(Object message) {
        log.fatal(message);
    }

    @Override
    public void fatal(String format, Object... args) {
        log.fatal(format(format, args));
    }

    @Override
    public void fatal(Object message, Exception exception) {
        log.fatal(message, exception);
    }

    private String format(String format, Object... args) {
        return String.format(format, args);
    }
}
