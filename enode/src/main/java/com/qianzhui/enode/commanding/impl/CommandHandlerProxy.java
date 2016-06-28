package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.commanding.*;
import com.qianzhui.enode.infrastructure.WrappedRuntimeException;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * Created by junbo_xu on 2016/3/25.
 */
public class CommandHandlerProxy implements ICommandHandlerProxy {

    private ICommandHandler _commandHandler;
    private MethodHandle _methodHandle;
    private Method _method;

    public CommandHandlerProxy(ICommandHandler commandHandler, MethodHandle methodHandle, Method method) {
        _commandHandler = commandHandler;
        _methodHandle = methodHandle;
        _method = method;
    }

    @Override
    public void handle(ICommandContext context, ICommand command) {
        try {
            _methodHandle.invoke(_commandHandler, context, command);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        } catch (Throwable throwable) {
            throw new WrappedRuntimeException(new RuntimeException(throwable));
        }
    }

    @Override
    public Object getInnerObject() {
        return _commandHandler;
    }
}
