package com.qianzhui.enode.domain.impl;

import com.qianzhui.enode.domain.IAggregateRoot;
import com.qianzhui.enode.domain.IAggregateSnapshotter;
import com.qianzhui.enode.domain.IAggregateStorage;

import javax.inject.Inject;

/**
 * Created by junbo_xu on 2016/4/2.
 */
public class SnapshotOnlyAggregateStorage implements IAggregateStorage {
    private final IAggregateSnapshotter _aggregateSnapshotter;

    @Inject
    public SnapshotOnlyAggregateStorage(IAggregateSnapshotter aggregateSnapshotter) {
        _aggregateSnapshotter = aggregateSnapshotter;
    }

    public <T extends IAggregateRoot> T get(Class<T> aggregateRootType, String aggregateRootId) {
        {
            if (aggregateRootType == null) throw new NullPointerException("aggregateRootType");
            if (aggregateRootId == null) throw new NullPointerException("aggregateRootId");

            T aggregateRoot = _aggregateSnapshotter.restoreFromSnapshot(aggregateRootType, aggregateRootId);

            if (aggregateRoot != null && (aggregateRoot.getClass() != aggregateRootType || aggregateRoot.uniqueId() != aggregateRootId)) {
                throw new RuntimeException(String.format("AggregateRoot recovery from snapshot is invalid as the aggregateRootType or aggregateRootId is not matched. Snapshot: [aggregateRootType:%s,aggregateRootId:%s], expected: [aggregateRootType:%s,aggregateRootId:%s]",
                        aggregateRoot.getClass(),
                        aggregateRoot.uniqueId(),
                        aggregateRootType,
                        aggregateRootId));
            }

            return aggregateRoot;
        }
    }
}
