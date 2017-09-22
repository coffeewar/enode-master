package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.eventing.IDomainEvent;
import com.qianzhui.enode.infrastructure.IMessageHandler;
import com.qianzhui.enode.infrastructure.IMessageHandlerProxy3;
import com.qianzhui.enode.infrastructure.IThreeMessageHandlerProvider;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Created by junbo_xu on 2016/4/3.
 */
public class DefaultThreeMessageHandlerProvider extends AbstractHandlerProvider<ManyType, IMessageHandlerProxy3, List<Class>> implements IThreeMessageHandlerProvider {
    @Override
    protected Class getHandlerType() {
        return IMessageHandler.class;
    }

    @Override
    protected ManyType getKey(Method method) {
        return new ManyType(Arrays.asList(method.getParameterTypes()));
    }

    @Override
    protected Class<? extends IMessageHandlerProxy3> getHandlerProxyImplementationType() {
        return MessageHandlerProxy3.class;
    }

    @Override
    protected boolean isHandlerSourceMatchKey(List<Class> handlerSource, ManyType key) {
        if(handlerSource.size() != 3)
            return false;

        for (Class type : key.getTypes()) {
            if (!handlerSource.stream().anyMatch(x -> x == type)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected boolean isHandleMethodMatch(Method method) {
        return method.getName().equals("handleAsync")
                && method.getParameterTypes().length == 3
                && IDomainEvent.class.isAssignableFrom(method.getParameterTypes()[0])
                && IDomainEvent.class.isAssignableFrom(method.getParameterTypes()[1])
                && IDomainEvent.class.isAssignableFrom(method.getParameterTypes()[2]);
    }
}
