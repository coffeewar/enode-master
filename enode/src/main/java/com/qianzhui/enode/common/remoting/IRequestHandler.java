package com.qianzhui.enode.common.remoting;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public interface IRequestHandler {
    RemotingResponse handleRequest(IRequestHandlerContext context, RemotingRequest remotingRequest);
}
