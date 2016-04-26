package com.qianzhui.enode.commanding;

/**
 * Created by junbo_xu on 2016/3/9.
 */
public interface ICommandKeyProvider {
    String getKey(ICommand command);
}
