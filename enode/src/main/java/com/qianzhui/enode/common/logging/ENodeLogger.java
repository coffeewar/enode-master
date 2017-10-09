package com.qianzhui.enode.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by xujunbo on 17-9-29.
 */
public class ENodeLogger {
    private static Logger log;

    static {
        log = createLogger("ENodeLog");
    }

    private static Logger createLogger(final String loggerName) {
        return LoggerFactory.getLogger(loggerName);
    }

    public static Logger getLog() {
        return log;
    }

    public static void setLog(Logger log) {
        ENodeLogger.log = log;
    }
}
