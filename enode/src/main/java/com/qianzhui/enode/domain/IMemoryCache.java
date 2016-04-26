package com.qianzhui.enode.domain;

import java.util.Collection;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public interface IMemoryCache {
    <T extends IAggregateRoot> T get(Object aggregateRootId, Class<T> aggregateRootType);

    void set(IAggregateRoot aggregateRoot);

    void refreshAggregateFromEventStore(String aggregateRootTypeName, String aggregateRootId);

    boolean remove(Object aggregateRootId);

    Collection<AggregateCacheInfo> getAll();
}
