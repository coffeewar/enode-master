package com.qianzhui.enode.commanding;

/**
 * Created by junbo_xu on 2016/4/22.
 */
public interface ICommandProcessor {
    void process(ProcessingCommand processingCommand);

    void start();

    void stop();
}
