package com.qianzhui.enode.infrastructure;

import com.qianzhui.enode.common.utilities.Ensure;
import com.qianzhui.enode.eventing.DomainEventStreamMessage;

/**
 * Created by junbo_xu on 2016/4/5.
 */
public class ProcessingDomainEventStreamMessage implements IProcessingMessage<ProcessingDomainEventStreamMessage, DomainEventStreamMessage, Boolean>, ISequenceProcessingMessage {
    private ProcessingMessageMailbox<ProcessingDomainEventStreamMessage, DomainEventStreamMessage, Boolean> _mailbox;
    private IMessageProcessContext _processContext;

    public DomainEventStreamMessage message;

    public ProcessingDomainEventStreamMessage(DomainEventStreamMessage message, IMessageProcessContext processContext) {
        this.message = message;
        _processContext = processContext;
    }

    public void setMailbox(ProcessingMessageMailbox<ProcessingDomainEventStreamMessage, DomainEventStreamMessage, Boolean> mailbox) {
        _mailbox = mailbox;
    }

    public void addToWaitingList() {
        Ensure.notNull(_mailbox, "_mailbox");
        _mailbox.addWaitingForRetryMessage(this);
    }

    public void setResult(Boolean result) {
        _processContext.notifyMessageProcessed();
        if (_mailbox != null) {
            _mailbox.completeMessage(this);
        }
    }

    @Override
    public DomainEventStreamMessage getMessage() {
        return message;
    }
}
