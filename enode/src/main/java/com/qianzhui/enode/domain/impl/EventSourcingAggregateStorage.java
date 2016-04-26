package com.qianzhui.enode.domain.impl;

import com.qianzhui.enode.domain.IAggregateRoot;
import com.qianzhui.enode.domain.IAggregateRootFactory;
import com.qianzhui.enode.domain.IAggregateSnapshotter;
import com.qianzhui.enode.domain.IAggregateStorage;
import com.qianzhui.enode.eventing.DomainEventStream;
import com.qianzhui.enode.eventing.IEventStore;
import com.qianzhui.enode.infrastructure.ITypeNameProvider;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by junbo_xu on 2016/4/1.
 */
public class EventSourcingAggregateStorage implements IAggregateStorage {
    private static final int minVersion = 1;
    private static final int maxVersion = Integer.MAX_VALUE;
    private final IAggregateRootFactory _aggregateRootFactory;
    private final IEventStore _eventStore;
    private final IAggregateSnapshotter _aggregateSnapshotter;
    private final ITypeNameProvider _typeNameProvider;

    @Inject
    public EventSourcingAggregateStorage(
            IAggregateRootFactory aggregateRootFactory,
            IEventStore eventStore,
            IAggregateSnapshotter aggregateSnapshotter,
            ITypeNameProvider typeNameProvider) {
        _aggregateRootFactory = aggregateRootFactory;
        _eventStore = eventStore;
        _aggregateSnapshotter = aggregateSnapshotter;
        _typeNameProvider = typeNameProvider;
    }

    public <T extends IAggregateRoot> T get(Class<T> aggregateRootType, String aggregateRootId) {
        if (aggregateRootType == null) throw new NullPointerException("aggregateRootType");
        if (aggregateRootId == null) throw new NullPointerException("aggregateRootId");


        T aggregateRoot = tryGetFromSnapshot(aggregateRootId, aggregateRootType);

        if (aggregateRoot != null) {
            return aggregateRoot;
        }

        String aggregateRootTypeName = _typeNameProvider.getTypeName(aggregateRootType);
        List<DomainEventStream> eventStreams = _eventStore.queryAggregateEvents(aggregateRootId, aggregateRootTypeName, minVersion, maxVersion);
        aggregateRoot = rebuildAggregateRoot(aggregateRootType, eventStreams);

        return aggregateRoot;
    }

    private <T extends IAggregateRoot> T tryGetFromSnapshot(String aggregateRootId, Class<T> aggregateRootType) {
        T aggregateRoot = _aggregateSnapshotter.restoreFromSnapshot(aggregateRootType, aggregateRootId);

        if (aggregateRoot == null) return null;

        if (aggregateRoot.getClass() != aggregateRootType || aggregateRoot.uniqueId() != aggregateRootId) {
            throw new RuntimeException(String.format("AggregateRoot recovery from snapshot is invalid as the aggregateRootType or aggregateRootId is not matched. Snapshot: [aggregateRootType:%s,aggregateRootId:%s], expected: [aggregateRootType:%s,aggregateRootId:%s]",
                    aggregateRoot.getClass(),
                    aggregateRoot.uniqueId(),
                    aggregateRootType,
                    aggregateRootId));
        }

        String aggregateRootTypeName = _typeNameProvider.getTypeName(aggregateRootType);
        List<DomainEventStream> eventStreamsAfterSnapshot = _eventStore.queryAggregateEvents(aggregateRootId, aggregateRootTypeName, aggregateRoot.version() + 1, Integer.MAX_VALUE);
        aggregateRoot.replayEvents(eventStreamsAfterSnapshot);

        return aggregateRoot;
    }

    private <T extends IAggregateRoot> T rebuildAggregateRoot(Class<T> aggregateRootType, List<DomainEventStream> eventStreams) {
        if (eventStreams == null || eventStreams.isEmpty()) return null;

        T aggregateRoot = _aggregateRootFactory.createAggregateRoot(aggregateRootType);
        aggregateRoot.replayEvents(eventStreams);

        return aggregateRoot;
    }
}
