package com.qianzhui.enode.common.remoting.exceptions;

import com.qianzhui.enode.common.remoting.RemotingRequest;

import java.net.SocketAddress;

/**
 * Created by junbo_xu on 2016/3/11.
 */
public class RemotingRequestException extends RuntimeException {
    private static final long serialVersionUID = 3998238386992619163L;

    public RemotingRequestException(SocketAddress serverEndPoint, RemotingRequest request, String errorMessage) {
        super(String.format("Send request %s to server [%s] failed, errorMessage:%s", request, serverEndPoint, errorMessage));
    }

    public RemotingRequestException(SocketAddress serverEndPoint, RemotingRequest request, Exception exception) {
        super(String.format("Send request %s to server [%s] failed.", request, serverEndPoint), exception);
    }
}
