package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.commanding.*;
import com.qianzhui.enode.infrastructure.impl.AbstractHandlerProvider;

import java.lang.reflect.Method;

/**
 * Created by junbo_xu on 2016/3/21.
 */
public class DefaultCommandHandlerProvider extends AbstractHandlerProvider<Class, ICommandHandlerProxy, Class> implements ICommandHandlerProvider {
    public DefaultCommandHandlerProvider(){
    }
    protected Class getHandlerType() {
        return ICommandHandler.class;
    }

    protected Class getKey(Method method) {
        return method.getParameterTypes()[1];
    }

    protected Class getHandlerProxyImplementationType() {
        return CommandHandlerProxy.class;
    }

    protected boolean isHandlerSourceMatchKey(Class handlerSource, Class key) {
        return key.isAssignableFrom(handlerSource);
    }

    protected boolean isHandleMethodMatchKey(Class[] argumentTypes, Class key) {
        return argumentTypes.length == 1 && argumentTypes[0] == key;
    }

    protected boolean isHandleMethodMatch(Method method) {
        return method.getName().equals("handle")
                && method.getParameterTypes().length == 2
                && method.getParameterTypes()[0] == ICommandContext.class
                && ICommand.class.isAssignableFrom(method.getParameterTypes()[1]);
    }
}
