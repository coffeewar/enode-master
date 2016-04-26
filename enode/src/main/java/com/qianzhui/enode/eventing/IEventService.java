package com.qianzhui.enode.eventing;

import com.qianzhui.enode.commanding.IProcessingCommandHandler;
import com.qianzhui.enode.commanding.ProcessingCommand;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public interface IEventService {
    void setProcessingCommandHandler(IProcessingCommandHandler processingCommandHandler);

    void commitDomainEventAsync(EventCommittingContext context);

    void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStream eventStream);
}
