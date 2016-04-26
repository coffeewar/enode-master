package com.qianzhui.enode.domain;

/**
 * Created by junbo_xu on 2016/4/1.
 */
public interface IAggregateSnapshotter {
    <T extends IAggregateRoot> T restoreFromSnapshot(Class<T> aggregateRootType, String aggregateRootId);
}
