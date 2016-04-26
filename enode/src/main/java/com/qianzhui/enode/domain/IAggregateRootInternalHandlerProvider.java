package com.qianzhui.enode.domain;

import com.qianzhui.enode.common.function.Action2;
import com.qianzhui.enode.eventing.IDomainEvent;

import java.lang.reflect.Method;

/**
 * Created by junbo_xu on 2016/2/24.
 */
public interface IAggregateRootInternalHandlerProvider {
//    Action<IAggregateRoot, IDomainEvent> GetInternalEventHandler(Type aggregateRootType, Type eventType);

    Action2<IAggregateRoot,IDomainEvent> getInternalEventHandler(Class<? extends IAggregateRoot> aggregateRootType, Class<? extends IDomainEvent> anEventType);
}
