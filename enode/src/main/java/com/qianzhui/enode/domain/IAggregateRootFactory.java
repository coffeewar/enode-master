package com.qianzhui.enode.domain;

/**
 * Defines a factory to create empty aggregate root.
 * Created by junbo_xu on 2016/4/1.
 */
public interface IAggregateRootFactory {

    /**
     * Create an empty aggregate root with the given type.
     *
     * @param aggregateRootType
     * @param <T>
     * @return
     */
    <T extends IAggregateRoot> T createAggregateRoot(Class<T> aggregateRootType);
}
