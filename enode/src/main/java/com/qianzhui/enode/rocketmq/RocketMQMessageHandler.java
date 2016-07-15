package com.qianzhui.enode.rocketmq;

import com.alibaba.rocketmq.common.message.MessageExt;
import com.qianzhui.enode.common.rocketmq.consumer.listener.CompletableConsumeConcurrentlyContext;

/**
 * Created by junbo_xu on 2016/4/15.
 */
public interface RocketMQMessageHandler {
    boolean isMatched(TopicTagData topicTagData);

    void handle(MessageExt message, CompletableConsumeConcurrentlyContext context);
}
