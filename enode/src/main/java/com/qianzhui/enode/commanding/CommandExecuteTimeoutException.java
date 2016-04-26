package com.qianzhui.enode.commanding;

/**
 * Created by junbo_xu on 2016/4/23.
 */
public class CommandExecuteTimeoutException extends RuntimeException {

    public CommandExecuteTimeoutException() {
        super();
    }

    public CommandExecuteTimeoutException(String message) {
        super(message);
    }

    public CommandExecuteTimeoutException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
