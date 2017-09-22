package com.qianzhui.enode.infrastructure;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public interface IProcessingMessageScheduler<X extends IProcessingMessage<X, Y>, Y extends IMessage> {
    void scheduleMessage(X processingMessage);

    void scheduleMailbox(ProcessingMessageMailbox<X, Y> mailbox);
}
