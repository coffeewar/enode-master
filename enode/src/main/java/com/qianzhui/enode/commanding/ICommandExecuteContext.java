package com.qianzhui.enode.commanding;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public interface ICommandExecuteContext extends ICommandContext, ITrackingContext {
    void onCommandExecuted(CommandResult commandResult);
}
