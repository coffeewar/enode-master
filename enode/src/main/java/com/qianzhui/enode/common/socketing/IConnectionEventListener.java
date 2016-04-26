package com.qianzhui.enode.common.socketing;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;

/**
 * Created by junbo_xu on 2016/3/4.
 */
public interface IConnectionEventListener {
    void onConnectionAccepted(Channel socketChannel);

    void onConnectionEstablished(Channel socketChannel);

    //void onConnectionFailed(SocketError socketError);
    void onConnectionFailed(Throwable cause);

    //void onConnectionClosed(Channel socketChannel, SocketError socketError);
    void onConnectionClosed(Channel socketChannel);
}
