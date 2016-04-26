package com.qianzhui.enode.infrastructure;

import javax.inject.Inject;

/**
 * Created by junbo_xu on 2016/4/5.
 */
public class ProcessingPublishableExceptionMessage implements IProcessingMessage<ProcessingPublishableExceptionMessage, IPublishableException, Boolean> {
    private ProcessingMessageMailbox<ProcessingPublishableExceptionMessage, IPublishableException, Boolean> _mailbox;
    private IMessageProcessContext _processContext;

    private IPublishableException message;

    @Inject
    public ProcessingPublishableExceptionMessage(IPublishableException message, IMessageProcessContext processContext) {
        this.message = message;
        _processContext = processContext;
    }

    @Override
    public void setMailbox(ProcessingMessageMailbox<ProcessingPublishableExceptionMessage, IPublishableException, Boolean> mailbox) {
        _mailbox = mailbox;
    }

    @Override
    public void setResult(Boolean result) {
        _processContext.notifyMessageProcessed();
        if (_mailbox != null) {
            _mailbox.completeMessage(this);
        }
    }

    @Override
    public IPublishableException getMessage() {
        return message;
    }
}
