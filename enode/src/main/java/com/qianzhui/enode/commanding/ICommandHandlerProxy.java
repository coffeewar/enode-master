package com.qianzhui.enode.commanding;

import com.qianzhui.enode.infrastructure.IObjectProxy;
import com.qianzhui.enode.infrastructure.MethodInvocation;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public interface ICommandHandlerProxy extends IObjectProxy, MethodInvocation{
    void handle(ICommandContext context, ICommand command);
}
