package com.qianzhui.enode.domain;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public interface IMemoryCache {
    /**
     * Get an aggregate from memory cache.
     *
     * @param aggregateRootId
     * @param aggregateRootType
     * @param <T>
     * @return
     */
    <T extends IAggregateRoot> T get(Object aggregateRootId, Class<T> aggregateRootType);

    /**
     * Set an aggregate to memory cache.
     *
     * @param aggregateRoot
     */
    void set(IAggregateRoot aggregateRoot);

    /**
     * Refresh the aggregate memory cache by replaying events of event store.
     *
     * @param aggregateRootTypeName
     * @param aggregateRootId
     */
    void refreshAggregateFromEventStore(String aggregateRootTypeName, String aggregateRootId);

    /**
     * Start background tasks.
     */
    void start();

    /**
     * Stop background tasks.
     */
    void stop();
}
