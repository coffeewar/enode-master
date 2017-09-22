package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.commanding.ICommand;
import com.qianzhui.enode.commanding.ICommandContext;
import com.qianzhui.enode.commanding.ICommandHandler;
import com.qianzhui.enode.commanding.ICommandHandlerProxy;
import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.infrastructure.WrappedRuntimeException;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * Created by junbo_xu on 2016/3/25.
 */
public class CommandHandlerProxy implements ICommandHandlerProxy {

    private Class _commandHandlerType;
    private ICommandHandler _commandHandler;
    private MethodHandle _methodHandle;
    private Method _method;

    public CommandHandlerProxy(Class commandHandlerType, ICommandHandler commandHandler, MethodHandle methodHandle, Method method) {
        _commandHandlerType = commandHandlerType;
        _commandHandler = commandHandler;
        _methodHandle = methodHandle;
        _method = method;
    }

    @Override
    public void handle(ICommandContext context, ICommand command) {
        ICommandHandler handler = (ICommandHandler) getInnerObject();
        try {
            _methodHandle.invoke(handler, context, command);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        } catch (Throwable throwable) {
            throw new WrappedRuntimeException(new RuntimeException(throwable));
        }
    }

    @Override
    public Object getInnerObject() {
        if (_commandHandler != null)
            return _commandHandler;

        return ObjectContainer.resolve(_commandHandlerType);
    }

    @Override
    public Method getMethod() {
        return _method;
    }
}
