package com.qianzhui.enode.infrastructure;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public interface IProcessingMessage<X extends IProcessingMessage<X,Y,Z>, Y extends IMessage, Z> {
    Y getMessage();
    void setMailbox(ProcessingMessageMailbox<X, Y, Z> mailbox);
    void setResult(Z result);
}
