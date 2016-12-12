package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.eventing.DomainEventStreamMessage;
import com.qianzhui.enode.infrastructure.IProcessingMessageHandler;
import com.qianzhui.enode.infrastructure.IProcessingMessageScheduler;
import com.qianzhui.enode.infrastructure.ProcessingDomainEventStreamMessage;

import javax.inject.Inject;

/**
 * Created by xujunbo on 16-12-12.
 */
public class DefaultDomainEventProcessor extends DefaultMessageProcessor<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> {
    @Inject
    public DefaultDomainEventProcessor(
            IProcessingMessageScheduler<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> processingMessageScheduler,
            IProcessingMessageHandler<ProcessingDomainEventStreamMessage, DomainEventStreamMessage> processingMessageHandler,
            ILoggerFactory loggerFactory) {
        super(processingMessageScheduler, processingMessageHandler, loggerFactory);
    }

    @Override
    public String getMessageName() {
        return "event message";
    }
}
