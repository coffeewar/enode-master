package com.qianzhui.enode.domain;

import com.qianzhui.enode.eventing.DomainEventStream;
import com.qianzhui.enode.eventing.IDomainEvent;

import java.util.List;

/**
 * Created by junbo_xu on 2016/2/24.
 */
public interface IAggregateRoot {
    String uniqueId();

    int version();

    List<IDomainEvent> getChanges();

    void acceptChanges(int newVersion);

    void replayEvents(List<DomainEventStream> eventStreams);
}
