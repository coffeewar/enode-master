package com.qianzhui.enode.domain.impl;

import com.qianzhui.enode.common.function.Action2;
import com.qianzhui.enode.domain.AggregateRoot;
import com.qianzhui.enode.domain.IAggregateRoot;
import com.qianzhui.enode.domain.IAggregateRootInternalHandlerProvider;
import com.qianzhui.enode.eventing.IDomainEvent;
import com.qianzhui.enode.infrastructure.IAssemblyInitializer;
import com.qianzhui.enode.infrastructure.TypeUtils;
import com.qianzhui.enode.infrastructure.WrappedRuntimeException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by junbo_xu on 2016/2/24.
 */
public class DefaultAggregateRootInternalHandlerProvider implements IAggregateRootInternalHandlerProvider, IAssemblyInitializer {

    private static final String HANDLE_METHOD_NAME = "handle";

    private Map<Class, Map<Class, Action2<IAggregateRoot, IDomainEvent>>> _mappings = new HashMap<>();

    @Override
    public void initialize(Set<Class<?>> componentTypes) {
        componentTypes.stream().filter(TypeUtils::isAggregateRoot).forEach(aggregateRootType ->
                Arrays.asList(aggregateRootType.getDeclaredMethods()).stream()
                        .filter(method ->
                                method.getName().equals(HANDLE_METHOD_NAME)
                                        && method.getParameterTypes().length == 1
                                        && IDomainEvent.class.isAssignableFrom(method.getParameterTypes()[0])
                        )
                        .forEach(method -> registerInternalHandler(aggregateRootType, method.getParameterTypes()[0], method)
                        )
        );
    }

    private void registerInternalHandler(Class aggregateRootType, Class eventType, Method method) {
        Map<Class, Action2<IAggregateRoot, IDomainEvent>> eventHandlerDic = _mappings.get(aggregateRootType);

        if (eventHandlerDic == null) {
            eventHandlerDic = new HashMap<>();
            _mappings.put(aggregateRootType, eventHandlerDic);
        }

        if (eventHandlerDic.containsKey(eventType)) {
            throw new RuntimeException(String.format("Found duplicated event handler on aggregate, aggregate type:%s, event type:%s", aggregateRootType.getClass().getName(), eventType.getClass().getName()));
        }

        try {
            //转换为MethodHandle提高效率
            //MethodHandle methodHandle = MethodHandles.lookup().findVirtual(aggregateRootType, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()));
            method.setAccessible(true);
            MethodHandle methodHandle = MethodHandles.lookup().unreflect(method);

            eventHandlerDic.put(eventType, (aggregateRoot, domainEvent) -> {
                try {
                    methodHandle.invoke(aggregateRoot, domainEvent);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            });
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public Action2<IAggregateRoot, IDomainEvent> getInternalEventHandler(Class<? extends IAggregateRoot> aggregateRootType,
                                                                         Class<? extends IDomainEvent> anEventType) {

        Map<Class, Action2<IAggregateRoot, IDomainEvent>> eventHandlerDic = _mappings.get(aggregateRootType);

        if (eventHandlerDic == null)
            return null;

        return eventHandlerDic.get(anEventType);
    }
}
