package com.qianzhui.enode.commanding;

import com.qianzhui.enode.domain.IAggregateRoot;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public interface ICommandContext {
    void add(IAggregateRoot aggregateRoot);

    <T extends IAggregateRoot> T get(Class<T> aggregateRootType, Object id);

    <T extends IAggregateRoot> T get(Class<T> aggregateRootType, Object id, boolean firstFromCache);

    void setResult(String result);

    String getResult();
}
