package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.commanding.*;
import com.qianzhui.enode.infrastructure.impl.AbstractHandlerProvider;

import java.lang.reflect.Method;

/**
 * Created by junbo_xu on 2016/3/31.
 */
public class DefaultCommandAsyncHandlerProvider extends AbstractHandlerProvider<Class, ICommandAsyncHandlerProxy, Class> implements ICommandAsyncHandlerProvider {
    @Override
    protected Class getHandlerType() {
        return ICommandAsyncHandler.class;
    }

    @Override
    protected Class getKey(Method method) {
        return method.getParameterTypes()[0];
    }

    @Override
    protected Class<? extends ICommandAsyncHandlerProxy> getHandlerProxyImplementationType() {
        return CommandAsyncHandlerProxy.class;
    }

    @Override
    protected boolean isHandlerSourceMatchKey(Class handlerSource, Class key) {
        return key.isAssignableFrom(handlerSource);
    }

    @Override
    protected boolean isHandleMethodMatch(Method method) {
        return method.getName().equals("handleAsync")
                && method.getParameterTypes().length == 1
                && ICommand.class.isAssignableFrom(method.getParameterTypes()[0]);
    }
}
