package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.infrastructure.IApplicationMessage;
import com.qianzhui.enode.infrastructure.IProcessingMessageHandler;
import com.qianzhui.enode.infrastructure.IProcessingMessageScheduler;
import com.qianzhui.enode.infrastructure.ProcessingApplicationMessage;

import javax.inject.Inject;

/**
 * Created by xujunbo on 16-12-12.
 */

public class DefaultApplicationMessageProcessor extends DefaultMessageProcessor<ProcessingApplicationMessage, IApplicationMessage> {

    @Inject
    public DefaultApplicationMessageProcessor(
            IProcessingMessageScheduler<ProcessingApplicationMessage, IApplicationMessage> processingMessageScheduler,
            IProcessingMessageHandler<ProcessingApplicationMessage, IApplicationMessage> processingMessageHandler,
            ILoggerFactory loggerFactory) {
        super(processingMessageScheduler, processingMessageHandler, loggerFactory);
    }

    @Override
    public String getMessageName() {
        return "application message";
    }
}
