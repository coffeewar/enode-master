package com.qianzhui.enode.common.remoting.exceptions;

import java.net.SocketAddress;

/**
 * Created by junbo_xu on 2016/3/11.
 */
public class RemotingServerUnAvailableException extends RuntimeException {
    private static final long serialVersionUID = 978288521866138954L;

    public RemotingServerUnAvailableException(SocketAddress serverEndPoint) {
        super(String.format("Remoting server is unavailable, server address:%s", serverEndPoint));
    }
}
