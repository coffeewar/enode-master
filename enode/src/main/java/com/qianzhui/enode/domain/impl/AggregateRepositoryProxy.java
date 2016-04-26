package com.qianzhui.enode.domain.impl;

import com.qianzhui.enode.domain.IAggregateRepository;
import com.qianzhui.enode.domain.IAggregateRepositoryProxy;
import com.qianzhui.enode.domain.IAggregateRoot;

/**
 * Created by junbo_xu on 2016/4/1.
 */
public class AggregateRepositoryProxy<TAggregateRoot extends IAggregateRoot> implements IAggregateRepositoryProxy {
    private final IAggregateRepository<TAggregateRoot> _aggregateRepository;

    public AggregateRepositoryProxy(IAggregateRepository<TAggregateRoot> aggregateRepository) {
        _aggregateRepository = aggregateRepository;
    }

    public Object getInnerObject() {
        return _aggregateRepository;
    }

    public IAggregateRoot get(String aggregateRootId) {
        return _aggregateRepository.get(aggregateRootId);
    }
}
