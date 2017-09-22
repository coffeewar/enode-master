package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.infrastructure.IMessage;
import com.qianzhui.enode.infrastructure.IMessageHandler;
import com.qianzhui.enode.infrastructure.IMessageHandlerProxy2;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/3/31.
 */
public class MessageHandlerProxy2 implements IMessageHandlerProxy2 {
    private Class _handlerType;
    private IMessageHandler _handler;
    private MethodHandle _methodHandle;
    private Method _method;
    private Class<?>[] _methodParameterTypes;

    public MessageHandlerProxy2(Class handlerType, IMessageHandler handler, MethodHandle methodHandle, Method method) {
        _handlerType = handlerType;
        _handler = handler;
        _methodHandle = methodHandle;
        _method = method;
        _methodParameterTypes = method.getParameterTypes();
    }

    @Override
    public CompletableFuture<AsyncTaskResult> handleAsync(IMessage message1, IMessage message2) {
        IMessageHandler handler = (IMessageHandler) getInnerObject();

        try {
            if(_methodParameterTypes[0].isAssignableFrom(message1.getClass())) {
                return (CompletableFuture<AsyncTaskResult>) _methodHandle.invoke(handler, message1, message2);
            } else {
                return (CompletableFuture<AsyncTaskResult>) _methodHandle.invoke(handler, message2, message1);
            }
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    @Override
    public Object getInnerObject() {
        if (_handler != null)
            return _handler;

        return ObjectContainer.resolve(_handlerType);
    }

    @Override
    public Method getMethod() {
        return _method;
    }
}
