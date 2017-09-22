package com.qianzhui.enode.infrastructure;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public interface IProcessingMessage<X extends IProcessingMessage<X,Y>, Y extends IMessage> {
    Y getMessage();
    void setMailbox(ProcessingMessageMailbox<X, Y> mailbox);
    void complete();
}
