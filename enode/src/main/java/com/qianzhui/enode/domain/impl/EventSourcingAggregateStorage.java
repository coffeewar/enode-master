package com.qianzhui.enode.domain.impl;

import com.qianzhui.enode.ENode;
import com.qianzhui.enode.common.io.IOHelper;
import com.qianzhui.enode.common.logging.ENodeLogger;
import com.qianzhui.enode.domain.IAggregateRoot;
import com.qianzhui.enode.domain.IAggregateRootFactory;
import com.qianzhui.enode.domain.IAggregateSnapshotter;
import com.qianzhui.enode.domain.IAggregateStorage;
import com.qianzhui.enode.eventing.DomainEventStream;
import com.qianzhui.enode.eventing.DomainEventStreamMessage;
import com.qianzhui.enode.eventing.IEventStore;
import com.qianzhui.enode.infrastructure.IMessagePublisher;
import com.qianzhui.enode.infrastructure.IPublishedVersionStore;
import com.qianzhui.enode.infrastructure.ITypeNameProvider;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by junbo_xu on 2016/4/1.
 */
public class EventSourcingAggregateStorage implements IAggregateStorage {
    private static final Logger _logger = ENodeLogger.getLog();

    private static final int minVersion = 1;
    private static final int maxVersion = Integer.MAX_VALUE;
    private final IAggregateRootFactory _aggregateRootFactory;
    private final IEventStore _eventStore;
    private final IPublishedVersionStore _publishedVersionStore;
    private final IMessagePublisher<DomainEventStreamMessage> _domainEventPublisher;
    private final IAggregateSnapshotter _aggregateSnapshotter;
    private final ITypeNameProvider _typeNameProvider;
    private final IOHelper _ioHelper;

    @Inject
    public EventSourcingAggregateStorage(
            IAggregateRootFactory aggregateRootFactory,
            IEventStore eventStore,
            IPublishedVersionStore publishedVersionStore,
            IMessagePublisher<DomainEventStreamMessage> domainEventPublisher,
            IAggregateSnapshotter aggregateSnapshotter,
            ITypeNameProvider typeNameProvider,
            IOHelper ioHelper) {
        _aggregateRootFactory = aggregateRootFactory;
        _eventStore = eventStore;
        _publishedVersionStore = publishedVersionStore;
        _domainEventPublisher = domainEventPublisher;
        _aggregateSnapshotter = aggregateSnapshotter;
        _typeNameProvider = typeNameProvider;
        _ioHelper = ioHelper;
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

        checkRepublishUnpublishedEventAsync(aggregateRoot, 0);

        return aggregateRoot;
    }

    private <T extends IAggregateRoot> T tryGetFromSnapshot(String aggregateRootId, Class<T> aggregateRootType) {
        T aggregateRoot = _aggregateSnapshotter.restoreFromSnapshot(aggregateRootType, aggregateRootId);

        if (aggregateRoot == null) return null;

        if (aggregateRoot.getClass() != aggregateRootType || !aggregateRoot.uniqueId().equals(aggregateRootId)) {
            throw new RuntimeException(String.format("AggregateRoot recovery from snapshot is invalid as the aggregateRootType or aggregateRootId is not matched. Snapshot: [aggregateRootType:%s,aggregateRootId:%s], expected: [aggregateRootType:%s,aggregateRootId:%s]",
                    aggregateRoot.getClass(),
                    aggregateRoot.uniqueId(),
                    aggregateRootType,
                    aggregateRootId));
        }

        String aggregateRootTypeName = _typeNameProvider.getTypeName(aggregateRootType);
        List<DomainEventStream> eventStreamsAfterSnapshot = _eventStore.queryAggregateEvents(aggregateRootId, aggregateRootTypeName, aggregateRoot.version() + 1, Integer.MAX_VALUE);
        aggregateRoot.replayEvents(eventStreamsAfterSnapshot);

        checkRepublishUnpublishedEventAsync(aggregateRoot, 0);

        return aggregateRoot;
    }

    private <T extends IAggregateRoot> T rebuildAggregateRoot(Class<T> aggregateRootType, List<DomainEventStream> eventStreams) {
        if (eventStreams == null || eventStreams.isEmpty()) return null;

        T aggregateRoot = _aggregateRootFactory.createAggregateRoot(aggregateRootType);
        aggregateRoot.replayEvents(eventStreams);

        return aggregateRoot;
    }

    private void checkRepublishUnpublishedEventAsync(IAggregateRoot aggregateRoot, int retryTimes) {
        _ioHelper.tryAsyncActionRecursively("CheckRepublishUnpublishedEventAsync",
                () -> _publishedVersionStore.getPublishedVersionAsync(ENode.getInstance().getSetting().getDomainEventStreamMessageHandlerName(),
                        _typeNameProvider.getTypeName(aggregateRoot.getClass()), aggregateRoot.uniqueId()),
                currentRetryTimes -> checkRepublishUnpublishedEventAsync(aggregateRoot, currentRetryTimes),
                result -> {
                    Integer publishedVersion = result.getData();
                    if (publishedVersion < aggregateRoot.version()) {
                        republishUnpublishedEvents(aggregateRoot, publishedVersion, 0);
                    }
                },
                () -> String.format("AggregateRootType:%s,AggId:%s", aggregateRoot.getClass().getName(), aggregateRoot.uniqueId()),
                errorMessage -> _logger.error("Check republish unpublished event async has unknown exception, the code should not be run to here, errorMessage: {}", errorMessage),
                retryTimes,
                true);
    }

    private void republishUnpublishedEvents(IAggregateRoot aggregateRoot, int publishedVersion, int retryTimes) {
        _ioHelper.tryAsyncActionRecursively("RepublishUnpublishedEvents",
                () -> _eventStore.queryAggregateEventsAsync(aggregateRoot.uniqueId(), _typeNameProvider.getTypeName(aggregateRoot.getClass()),
                        publishedVersion + 1, aggregateRoot.version()),
                currentRetryTimes -> republishUnpublishedEvents(aggregateRoot, publishedVersion, currentRetryTimes),
                result ->
                        result.getData().stream().map(
                                eventStream -> new DomainEventStreamMessage(eventStream.commandId(), eventStream.aggregateRootId(),
                                        eventStream.version(), eventStream.aggregateRootTypeName(), eventStream.events(), eventStream.items())
                        ).forEach(eventStreamMessage -> republishUnpublishedEvent(eventStreamMessage, 0))
                ,
                () -> String.format("AggregateRootType:%s,AggId:%s", aggregateRoot.getClass().getName(), aggregateRoot.uniqueId()),
                errorMessage -> _logger.error("Republish unpublished event async has unknown exception, the code should not be run to here, errorMessage: {}", errorMessage),
                retryTimes,
                true
        );
    }

    private void republishUnpublishedEvent(DomainEventStreamMessage eventStreamMessage, int retryTimes) {
        _ioHelper.tryAsyncActionRecursively("RepublishUnpublishedEvent",
                () -> _domainEventPublisher.publishAsync(eventStreamMessage),
                currentRetryTimes -> republishUnpublishedEvent(eventStreamMessage, currentRetryTimes),
                result -> {

                },
                () -> String.format("[eventStream:%s]", eventStreamMessage),
                errorMessage -> _logger.error("Republish unpublished event async has unknown exception, the code should not be run to here, errorMessage: {}", errorMessage),
                retryTimes,
                true
        );
    }
}
