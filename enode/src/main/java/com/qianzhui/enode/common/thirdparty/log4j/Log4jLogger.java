package com.qianzhui.enode.common.thirdparty.log4j;

import com.qianzhui.enode.common.logging.ILogger;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Created by junbo_xu on 2016/3/1.
 */
public class Log4jLogger implements ILogger {

    private final static String FQCN = Log4jLogger.class.getName();

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
        log.log(FQCN, Level.DEBUG, message, null);
    }

    @Override
    public void debug(String format, Object... args) {
        log.log(FQCN, Level.DEBUG, format(format, args), null);
    }

    @Override
    public void debug(Object message, Exception exception) {
        log.log(FQCN, Level.DEBUG, message, exception);
    }

    @Override
    public void debug(Object message, Throwable exception) {
        log.log(FQCN, Level.DEBUG, message, exception);
    }

    @Override
    public void info(Object message) {
        log.log(FQCN, Level.INFO, message, null);
    }

    @Override
    public void info(String format, Object... args) {
        log.log(FQCN, Level.INFO, format(format, args), null);
    }

    @Override
    public void info(Object message, Exception exception) {
        log.log(FQCN, Level.INFO, message, exception);
    }

    @Override
    public void info(Object message, Throwable exception) {
        log.log(FQCN, Level.INFO, message, exception);
    }

    @Override
    public void error(Object message) {
        log.log(FQCN, Level.ERROR, message, null);
    }

    @Override
    public void error(String format, Object... args) {
        log.log(FQCN, Level.ERROR, format(format, args), null);
    }

    @Override
    public void error(Object message, Exception exception) {
        log.log(FQCN, Level.ERROR, message, exception);
    }

    public void error(Object message, Throwable exception) {
        log.log(FQCN, Level.ERROR, message, exception);
    }

    @Override
    public void warn(Object message) {
        log.log(FQCN, Level.WARN, message, null);
    }

    @Override
    public void warn(String format, Object... args) {
        log.log(FQCN, Level.WARN, format(format, args), null);
    }

    @Override
    public void warn(Object message, Exception exception) {
        log.log(FQCN, Level.WARN, message, exception);
    }

    @Override
    public void warn(Object message, Throwable exception) {
        log.log(FQCN, Level.WARN, message, exception);
    }

    @Override
    public void fatal(Object message) {
        log.log(FQCN, Level.FATAL, message, null);
    }

    @Override
    public void fatal(String format, Object... args) {
        log.log(FQCN, Level.FATAL, format(format, args), null);
    }

    @Override
    public void fatal(Object message, Exception exception) {
        log.log(FQCN, Level.FATAL, message, exception);
    }

    @Override
    public void fatal(Object message, Throwable exception) {
        log.log(FQCN, Level.FATAL, message, exception);
    }

    private String format(String format, Object... args) {
        return String.format(format, args);
    }
}
