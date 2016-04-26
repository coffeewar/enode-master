package com.qianzhui.enode.common.socketing;

import java.net.SocketAddress;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public interface ITcpConnection {
    boolean isConnected();

    SocketAddress getLocalEndPoint();

    SocketAddress getRemotingEndPoint();

    void queueMessage(byte[] message);

    void close();
}
