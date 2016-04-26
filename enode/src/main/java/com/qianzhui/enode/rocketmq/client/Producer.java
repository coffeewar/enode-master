package com.qianzhui.enode.rocketmq.client;

import com.alibaba.rocketmq.client.producer.MessageQueueSelector;
import com.alibaba.rocketmq.client.producer.SendCallback;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;

/**
 * Created by junbo_xu on 2016/4/20.
 */
public interface Producer {
    /**
     * 启动服务
     */
    public void start();


    /**
     * 关闭服务
     */
    public void shutdown();

    SendResult send(final Message msg, final MessageQueueSelector selector, final Object arg);

    void send(final Message msg, final MessageQueueSelector selector, final Object arg,
              final SendCallback sendCallback);
}
