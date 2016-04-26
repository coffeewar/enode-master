package com.qianzhui.enode.common.function;

import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.remoting.exception.RemotingException;

import java.io.IOException;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public interface Action {
    void apply() throws Exception;
}
