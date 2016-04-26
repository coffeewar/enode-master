package com.qianzhui.enode.rocketmq.client.impl;

import com.qianzhui.enode.rocketmq.client.Consumer;
import com.qianzhui.enode.rocketmq.client.Producer;
import com.qianzhui.enode.rocketmq.client.RocketMQFactory;

import java.util.Properties;

/**
 * Created by junbo_xu on 2016/4/20.
 */
public class DefaultRocketMQFactory implements RocketMQFactory {

    @Override
    public Producer createProducer(Properties properties) {
        return null;
    }

    @Override
    public Consumer createPushConsumer(Properties properties) {
        return null;
    }
}
