package com.qianzhui.enode.common.remoting;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public interface RemotingRequestType {
    short Async = 1;
    short Oneway = 2;
    short Callback = 3;
}
