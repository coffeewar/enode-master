package com.qianzhui.enode.rocketmq.client.ons;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;
import com.qianzhui.enode.rocketmq.client.AbstractConsumer;
import com.qianzhui.enode.rocketmq.client.MQClientInitializer;
import com.qianzhui.enode.rocketmq.client.Consumer;
import com.qianzhui.enode.rocketmq.client.RocketMQClientException;

import java.util.Properties;

/**
 * Created by junbo_xu on 2016/4/20.
 */
public class ONSConsumerImpl extends AbstractConsumer implements Consumer {
    public ONSConsumerImpl(Properties properties) {
        super(properties, new ONSClientInitializer());
    }

    protected DefaultMQPushConsumer initConsumer(Properties properties, MQClientInitializer mqClientInitializer) {
        DefaultMQPushConsumer defaultMQPushConsumer = new DefaultMQPushConsumer(new ClientRPCHook(((ONSClientInitializer) mqClientInitializer).sessionCredentials));

        String consumerGroup = properties.getProperty(PropertyKeyConst.ConsumerId);
        if (null == consumerGroup) {
            throw new RocketMQClientException("\'ConsumerId\' property is null");
        }

        String messageModel =
                properties.getProperty(PropertyKeyConst.MessageModel, PropertyValueConst.CLUSTERING);
        defaultMQPushConsumer.setMessageModel(MessageModel.valueOf(messageModel));
        defaultMQPushConsumer.setConsumerGroup(consumerGroup);
        defaultMQPushConsumer.setInstanceName(mqClientInitializer.buildIntanceName());
        defaultMQPushConsumer.setNamesrvAddr(mqClientInitializer.getNameServerAddr());

        if (properties.containsKey(PropertyKeyConst.ConsumeThreadNums)) {
            defaultMQPushConsumer.setConsumeThreadMin(Integer.valueOf(properties.get(
                    PropertyKeyConst.ConsumeThreadNums).toString()));

            defaultMQPushConsumer.setConsumeThreadMax(Integer.valueOf(properties.get(
                    PropertyKeyConst.ConsumeThreadNums).toString()));
        }

        return defaultMQPushConsumer;
    }
}
