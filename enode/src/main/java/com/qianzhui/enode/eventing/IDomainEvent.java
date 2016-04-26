package com.qianzhui.enode.eventing;

import com.qianzhui.enode.infrastructure.ISequenceMessage;

/**
 * Created by junbo_xu on 2016/2/24.
 */
public interface IDomainEvent<TAggregateRootId> extends ISequenceMessage {
    TAggregateRootId aggregateRootId ();

    void setAggregateRootId(TAggregateRootId aggregateRootId);
}
