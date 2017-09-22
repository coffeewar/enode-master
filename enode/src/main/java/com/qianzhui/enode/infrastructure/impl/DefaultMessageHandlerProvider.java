package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.infrastructure.IMessage;
import com.qianzhui.enode.infrastructure.IMessageHandler;
import com.qianzhui.enode.infrastructure.IMessageHandlerProvider;
import com.qianzhui.enode.infrastructure.IMessageHandlerProxy1;

import java.lang.reflect.Method;

/**
 * Created by junbo_xu on 2016/3/31.
 */
public class DefaultMessageHandlerProvider extends AbstractHandlerProvider<Class, IMessageHandlerProxy1, Class> implements IMessageHandlerProvider {
    @Override
    protected Class getHandlerType() {
        return IMessageHandler.class;
    }

    @Override
    protected Class getKey(Method method) {
        return method.getParameterTypes()[0];
    }

    @Override
    protected Class<? extends IMessageHandlerProxy1> getHandlerProxyImplementationType() {
        return MessageHandlerProxy1.class;
    }

    @Override
    protected boolean isHandlerSourceMatchKey(Class handlerSource, Class key) {
        return key.isAssignableFrom(handlerSource);
    }

    @Override
    protected boolean isHandleMethodMatch(Method method) {
        return method.getName().equals("handleAsync")
                && method.getParameterTypes().length == 1
                && IMessage.class.isAssignableFrom(method.getParameterTypes()[0]);
    }
}
