package com.qianzhui.enode.common.logging;

/**
 * Created by junbo_xu on 2016/3/31.
 */
public class EmptyLogger implements ILogger {
    public boolean isDebugEnabled() {
        return false;
    }

    public void debug(Object message) {
    }

    public void debug(String format, Object[] args) {
    }

    public void debug(Object message, Exception exception) {
    }

    @Override
    public void debug(Object message, Throwable exception) {

    }

    public void info(Object message) {
    }

    public void info(String format, Object[] args) {
    }

    public void info(Object message, Exception exception) {
    }

    @Override
    public void info(Object message, Throwable exception) {

    }

    public void error(Object message) {
    }

    public void error(String format, Object[] args) {
    }

    public void error(Object message, Exception exception) {
    }

    @Override
    public void error(Object message, Throwable exception) {

    }

    public void warn(Object message) {
    }

    public void warn(String format, Object[] args) {
    }

    public void warn(Object message, Exception exception) {
    }

    @Override
    public void warn(Object message, Throwable exception) {

    }

    public void fatal(Object message) {
    }

    public void fatal(String format, Object[] args) {
    }

    public void fatal(Object message, Exception exception) {
    }

    @Override
    public void fatal(Object message, Throwable exception) {

    }
}
