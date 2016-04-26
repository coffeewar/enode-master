package com.qianzhui.enode.rocketmq.client;

import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;

/**
 * Created by junbo_xu on 2016/4/20.
 */
public interface Consumer {
    void start();

    void shutdown();

    void registerMessageListener(final MessageListenerConcurrently messageListener);

    void subscribe(final String topic, final String subExpression);

    void unsubscribe(final String topic);
}
