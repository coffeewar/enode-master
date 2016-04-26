package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.infrastructure.IAssemblyInitializer;
import com.qianzhui.enode.infrastructure.IObjectProxy;
import com.qianzhui.enode.infrastructure.MessageHandlerData;
import com.qianzhui.enode.infrastructure.Priority;
import org.reflections.ReflectionUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by junbo_xu on 2016/3/21.
 */
public abstract class AbstractHandlerProvider<TKey, THandlerProxyInterface extends IObjectProxy, THandlerSource> implements IAssemblyInitializer {
    private Map<TKey, List<THandlerProxyInterface>> _handlerDict = new HashMap<>();
    private Map<TKey, MessageHandlerData<THandlerProxyInterface>> _messageHandlerDict = new HashMap<>();
    private MethodHandles.Lookup lookup = MethodHandles.lookup();

    //ICommandHandler、ICommandAsyncHandler、IMessageHandler、IMessageHandler<,>、IMessageHandler<,,>
    protected abstract Class getHandlerType();

    //    protected abstract TKey getKey(Class handlerInterfaceType);
    protected abstract TKey getKey(Method method);

    protected abstract Class<? extends THandlerProxyInterface> getHandlerProxyImplementationType();

    protected abstract boolean isHandlerSourceMatchKey(THandlerSource handlerSource, TKey key);

    protected abstract boolean isHandleMethodMatchKey(Class[] argumentTypes, TKey key);

    protected abstract boolean isHandleMethodMatch(Method method);

    @Override
    public void initialize(Set<Class<?>> componentTypes) {
        componentTypes.stream().filter(this::isHandlerType).forEach(this::registerHandler);
        initializeHandlerPriority();
    }

    public List<MessageHandlerData<THandlerProxyInterface>> getHandlers(THandlerSource source) {
        List<MessageHandlerData<THandlerProxyInterface>> handlerDataList = new ArrayList<>();

        _messageHandlerDict.keySet().stream()
                .filter(key -> isHandlerSourceMatchKey(source, key))
                .forEach(key -> handlerDataList.add(_messageHandlerDict.get(key)));

        return handlerDataList;
    }

    private void initializeHandlerPriority() {
        _handlerDict.entrySet().stream().forEach(entry -> {
            TKey key = entry.getKey();
            List<THandlerProxyInterface> handlers = entry.getValue();

            MessageHandlerData<THandlerProxyInterface> handlerData = new MessageHandlerData<>();
            List<THandlerProxyInterface> listHandlers = new ArrayList<>();
            Map<THandlerProxyInterface, Integer> queueHandlerDict = new HashMap<>();

            handlers.forEach(handler -> {
                int priority = getHandleMethodPriority(handler, key);

                if (priority == 0) {
                    listHandlers.add(handler);
                } else {
                    queueHandlerDict.put(handler, priority);
                }
            });

            handlerData.AllHandlers = handlers;
            handlerData.ListHandlers = listHandlers;
            handlerData.QueuedHandlers = queueHandlerDict.entrySet().stream().sorted((o1, o2) -> o1.getValue() - o2.getValue()).map(x -> x.getKey()).collect(Collectors.toList());

            _messageHandlerDict.put(key, handlerData);
        });
    }

    private int getHandleMethodPriority(THandlerProxyInterface handler, TKey key) {
        int priority = 0;
        List<Method> handleAsyncMethods = Arrays.asList(handler.getInnerObject().getClass().getMethods()).stream().filter(x -> x.getName().equals("handleAsync")).collect(Collectors.toList());

        for (int i = 0, len = handleAsyncMethods.size(); i < len; i++) {
            Method method = handleAsyncMethods.get(i);
            Class<?>[] argumentTypes = method.getParameterTypes();
            if (isHandleMethodMatchKey(argumentTypes, key)) {
                Priority methodPriority = method.getAnnotation(Priority.class);
                if (methodPriority != null) {
                    priority = methodPriority.value();
                }

                if (priority == 0) {
                    Priority classPriority = handler.getInnerObject().getClass().getAnnotation(Priority.class);
                    if (classPriority != null)
                        priority = classPriority.value();
                }
            }
        }

        return priority;
    }

    private boolean isHandlerType(Class type) {
        return type != null && !type.isInterface() && !Modifier.isAbstract(type.getModifiers()) && getHandlerType().isAssignableFrom(type);
    }

    private void registerHandler(Class handlerType) {
        Object handleObj = ObjectContainer.resolve(handlerType);

        Set<Method> handleMethods = ReflectionUtils.getMethods(handlerType, this::isHandleMethodMatch);

        handleMethods.stream().forEach(method -> {
            try {
                //反射Method转换为MethodHandle,提高效率

                MethodHandle handleMethod = lookup.findVirtual(handlerType, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()));
                TKey key = getKey(method);

                List<THandlerProxyInterface> handlers = _handlerDict.get(key);
                if (handlers == null) {
                    handlers = new ArrayList<>();
                    _handlerDict.put(key, handlers);
                }

                //TODO handle duplicate message
                /*var handler = handlers.SingleOrDefault(x => x.GetInnerObject().GetType() == handlerType);
                if (handler != null)
                {
                    throw new InvalidOperationException("Handler cannot handle duplicate message, handlerType:" + handlerType);
                }*/

                THandlerProxyInterface handlerProxy = getHandlerProxyImplementationType().getConstructor(getHandlerType(), MethodHandle.class, Method.class).newInstance(handleObj, handleMethod, method);
                handlers.add(handlerProxy);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

//    x -> {
//        if (!x.getName().equals("handle"))
//            return false;
//
//        Class<?>[] parameterTypes = x.getParameterTypes();
//        if (parameterTypes.length != 2)
//            return false;
//
//        return parameterTypes[0] == ICommandContext.class && ICommand.class.isAssignableFrom(parameterTypes[1]);
//    }
}
