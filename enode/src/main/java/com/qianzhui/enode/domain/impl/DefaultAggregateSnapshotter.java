package com.qianzhui.enode.domain.impl;

import com.qianzhui.enode.domain.IAggregateRepositoryProvider;
import com.qianzhui.enode.domain.IAggregateRepositoryProxy;
import com.qianzhui.enode.domain.IAggregateRoot;
import com.qianzhui.enode.domain.IAggregateSnapshotter;

import javax.inject.Inject;

/**
 * Created by junbo_xu on 2016/4/1.
 */
public class DefaultAggregateSnapshotter implements IAggregateSnapshotter {
    private final IAggregateRepositoryProvider _aggregateRepositoryProvider;

    @Inject
    public DefaultAggregateSnapshotter(IAggregateRepositoryProvider aggregateRepositoryProvider) {
        _aggregateRepositoryProvider = aggregateRepositoryProvider;
    }

    public IAggregateRoot restoreFromSnapshot(Class aggregateRootType, String aggregateRootId) {
        IAggregateRepositoryProxy aggregateRepository = _aggregateRepositoryProvider.getRepository(aggregateRootType);
        if (aggregateRepository != null) {
            return aggregateRepository.get(aggregateRootId);
        }
        return null;
    }
}
