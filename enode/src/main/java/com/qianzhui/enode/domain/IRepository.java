package com.qianzhui.enode.domain;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public interface IRepository {
    <T extends IAggregateRoot> T get(Class<T> aggregateRootType, Object aggregateRootId);
}
