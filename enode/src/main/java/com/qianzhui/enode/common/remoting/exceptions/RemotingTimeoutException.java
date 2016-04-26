package com.qianzhui.enode.common.remoting.exceptions;

import com.qianzhui.enode.common.remoting.RemotingRequest;

import java.net.SocketAddress;

/**
 * Created by junbo_xu on 2016/3/11.
 */
public class RemotingTimeoutException extends RuntimeException {
    private static final long serialVersionUID = 500432114037685473L;

    public RemotingTimeoutException(SocketAddress serverEndPoint, RemotingRequest request, long timeoutMillis) {
        super(String.format("Wait response from server[%s] timeout, request:%s, timeoutMillis:%dms", serverEndPoint, request, timeoutMillis));
    }
}
