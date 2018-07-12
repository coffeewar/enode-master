package com.qianzhui.enode.eventing;

import com.qianzhui.enode.common.io.AsyncTaskResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public interface IEventStore {
    boolean isSupportBatchAppendEvent();

    List<DomainEventStream> queryAggregateEvents(String aggregateRootId, String aggregateRootTypeName, int minVersion, int maxVersion);

    AsyncTaskResult<EventAppendResult> batchAppend(List<DomainEventStream> eventStreams);

    AsyncTaskResult<EventAppendResult> append(DomainEventStream eventStream);

    CompletableFuture<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, int version);

    CompletableFuture<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, String commandId);

    CompletableFuture<AsyncTaskResult<List<DomainEventStream>>> queryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, int minVersion, int maxVersion);
}
