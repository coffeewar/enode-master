package com.qianzhui.enode.rocketmq;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.common.message.MessageExt;

/**
 * Created by junbo_xu on 2016/4/15.
 */
public interface RocketMQMessageHandler {
    boolean isMatched(TopicTagData topicTagData);

    ConsumeConcurrentlyStatus handle(MessageExt message, ConsumeConcurrentlyContext context);
}
