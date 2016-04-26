package com.qianzhui.enode.domain;

/**
 * Created by junbo_xu on 2016/4/1.
 */
public interface IAggregateStorage {
    <T extends IAggregateRoot> T get(Class<T> aggregateRootType, String aggregateRootId);
}
