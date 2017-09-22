package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.infrastructure.IProcessingMessageHandler;
import com.qianzhui.enode.infrastructure.IProcessingMessageScheduler;
import com.qianzhui.enode.infrastructure.IPublishableException;
import com.qianzhui.enode.infrastructure.ProcessingPublishableExceptionMessage;

import javax.inject.Inject;

/**
 * Created by xujunbo on 16-12-12.
 */
public class DefaultPublishableExceptionProcessor extends DefaultMessageProcessor<ProcessingPublishableExceptionMessage, IPublishableException> {
    @Inject
    public DefaultPublishableExceptionProcessor(
            IProcessingMessageScheduler<ProcessingPublishableExceptionMessage, IPublishableException> processingMessageScheduler,
            IProcessingMessageHandler<ProcessingPublishableExceptionMessage, IPublishableException> processingMessageHandler,
            ILoggerFactory loggerFactory) {
        super(processingMessageScheduler, processingMessageHandler, loggerFactory);
    }

    @Override
    public String getMessageName() {
        return "exception message";
    }
}

