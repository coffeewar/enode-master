package com.qianzhui.enode.remoting;

import com.qianzhui.enode.remoting.exception.RemotingCommandException;

/**
 * Created by xujunbo on 18-1-19.
 */
public interface CommandCustomHeader {
    void checkFields() throws RemotingCommandException;
}
