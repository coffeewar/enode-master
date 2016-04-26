package com.qianzhui.enode.eventing;

import com.qianzhui.enode.infrastructure.SequenceMessage;

/**
 * Created by junbo_xu on 2016/2/24.
 */
public abstract class DomainEvent<TAggregateRootId> extends SequenceMessage<TAggregateRootId>
        implements IDomainEvent<TAggregateRootId> {

}
