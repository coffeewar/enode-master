package com.qianzhui.enode.domain;

/**
 * Created by junbo_xu on 2016/4/1.
 */
public interface IAggregateRepository<TAggregateRoot extends IAggregateRoot> {
    TAggregateRoot get(String aggregateRootId);
}
