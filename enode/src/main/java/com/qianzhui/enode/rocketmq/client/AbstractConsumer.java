package com.qianzhui.enode.rocketmq.client;

import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.qianzhui.enode.common.rocketmq.consumer.CompletableDefaultMQPushConsumer;
import com.qianzhui.enode.common.rocketmq.consumer.listener.CompletableMessageListenerConcurrently;

import java.util.Properties;

/**
 * Created by junbo_xu on 2016/4/21.
 */
public abstract class AbstractConsumer {

    private final CompletableDefaultMQPushConsumer defaultMQPushConsumer;

    abstract protected CompletableDefaultMQPushConsumer initConsumer(Properties properties, MQClientInitializer mqClientInitializer);

    protected AbstractConsumer(Properties properties, MQClientInitializer mqClientInitializer) {
        mqClientInitializer.init(properties);

        this.defaultMQPushConsumer = initConsumer(properties, mqClientInitializer);
    }

    public void start() {
        try {
            this.defaultMQPushConsumer.start();
        } catch (Exception e) {
            throw new RocketMQClientException(e.getMessage(), e);
        }
    }

    public void shutdown() {
        this.defaultMQPushConsumer.shutdown();
    }

    public void registerMessageListener(MessageListenerConcurrently messageListener) {
        if (null == messageListener) {
            throw new RocketMQClientException("listener is null");
        }

        this.defaultMQPushConsumer.registerMessageListener(messageListener);
    }

    public void registerMessageListener(CompletableMessageListenerConcurrently messageListener) {
        if (null == messageListener) {
            throw new RocketMQClientException("listener is null");
        }

        this.defaultMQPushConsumer.registerMessageListener(messageListener);
    }

    public void subscribe(String topic, String subExpression) {
        if (null == topic) {
            throw new RocketMQClientException("topic is null");
        }

        try {
            this.defaultMQPushConsumer.subscribe(topic, subExpression);
        } catch (MQClientException e) {
            throw new RocketMQClientException("defaultMQPushConsumer subscribe exception", e);
        }
    }

    public void unsubscribe(String topic) {
        if (null != topic) {
            this.defaultMQPushConsumer.unsubscribe(topic);
        }
    }
}
