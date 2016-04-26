package com.qianzhui.enode.domain.impl;

import com.qianzhui.enode.domain.IAggregateRoot;
import com.qianzhui.enode.domain.IAggregateStorage;
import com.qianzhui.enode.domain.IMemoryCache;
import com.qianzhui.enode.domain.IRepository;

import javax.inject.Inject;

/**
 * Created by junbo_xu on 2016/4/1.
 */
public class DefaultRepository implements IRepository {
    private final IMemoryCache _memoryCache;
    private final IAggregateStorage _aggregateRootStorage;

    @Inject
    public DefaultRepository(IMemoryCache memoryCache, IAggregateStorage aggregateRootStorage) {
        _memoryCache = memoryCache;
        _aggregateRootStorage = aggregateRootStorage;
    }

    public <T extends IAggregateRoot> T get(Class<T> aggregateRootType, Object aggregateRootId) {
        if (aggregateRootType == null) {
            throw new NullPointerException("aggregateRootType");
        }
        if (aggregateRootId == null) {
            throw new NullPointerException("aggregateRootId");
        }

        T aggregateRoot = _memoryCache.get(aggregateRootId, aggregateRootType);
        return aggregateRoot == null ? _aggregateRootStorage.get(aggregateRootType, aggregateRootId.toString()) : aggregateRoot;
    }
}
