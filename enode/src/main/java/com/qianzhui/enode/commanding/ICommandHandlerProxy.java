package com.qianzhui.enode.commanding;

import com.qianzhui.enode.infrastructure.IObjectProxy;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public interface ICommandHandlerProxy extends IObjectProxy {
    void handle(ICommandContext context, ICommand command);
}
