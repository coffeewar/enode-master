package com.qianzhui.enode.infrastructure;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public interface IProcessingMessageHandler<X extends IProcessingMessage<X,Y,Z>, Y extends IMessage, Z> {
    void handleAsync(X processingMessage);
}
