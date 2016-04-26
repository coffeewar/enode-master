package com.qianzhui.enode.rocketmq;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.qianzhui.enode.infrastructure.IMessageProcessContext;

/**
 * Created by junbo_xu on 2016/4/6.
 */
public class RocketMQProcessContext implements IMessageProcessContext {
    protected final MessageExt _queueMessage;
    protected final ConsumeConcurrentlyContext _messageContext;

    public RocketMQProcessContext(MessageExt queueMessage, ConsumeConcurrentlyContext messageContext) {
        _queueMessage = queueMessage;
        _messageContext = messageContext;
    }

    public void notifyMessageProcessed() {
    }
}
