package com.qianzhui.enode.common.remoting;

import io.netty.channel.Channel;

import java.util.function.Consumer;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public interface IRequestHandlerContext {
    Channel getChannel();

    Consumer<RemotingResponse> getSendRemotingResponse();
}
