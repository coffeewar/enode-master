package com.qianzhui.enode.commanding;

/**
 * Created by junbo_xu on 2016/3/2.
 */
public interface ICommandRoutingKeyProvider {
    String getRoutingKey(ICommand command);
}
