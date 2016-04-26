package com.qianzhui.enode.common.remoting;

import io.netty.channel.Channel;

import java.util.function.Consumer;

/**
 * Created by junbo_xu on 2016/3/11.
 */
public class SocketRequestHandlerContext implements IRequestHandlerContext {
    public Channel channel;
    public Consumer<RemotingResponse> sendRemotingResponse;

    public SocketRequestHandlerContext(Channel channel, Consumer<byte[]> sendReplyAction) {
        this.channel = channel;

        sendRemotingResponse = remotingResponse -> sendReplyAction.accept(RemotingUtil.buildResponseMessage(remotingResponse));
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public Consumer<RemotingResponse> getSendRemotingResponse() {
        return sendRemotingResponse;
    }
}
