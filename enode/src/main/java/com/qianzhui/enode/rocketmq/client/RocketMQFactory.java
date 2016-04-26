package com.qianzhui.enode.rocketmq.client;

import java.util.Properties;

/**
 * Created by junbo_xu on 2016/4/19.
 */
public interface RocketMQFactory {
    Producer createProducer(final Properties properties);

    Consumer createPushConsumer(final Properties properties);
}
