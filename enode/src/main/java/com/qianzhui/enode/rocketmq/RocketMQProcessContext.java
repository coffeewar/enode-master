package com.qianzhui.enode.rocketmq;

import com.alibaba.rocketmq.common.message.MessageExt;
import com.qianzhui.enode.common.rocketmq.consumer.listener.CompletableConsumeConcurrentlyContext;
import com.qianzhui.enode.infrastructure.IMessageProcessContext;

/**
 * Created by junbo_xu on 2016/4/6.
 */
public class RocketMQProcessContext implements IMessageProcessContext {
    protected final MessageExt _queueMessage;
    protected final CompletableConsumeConcurrentlyContext _messageContext;

    public RocketMQProcessContext(MessageExt queueMessage, CompletableConsumeConcurrentlyContext messageContext) {
        _queueMessage = queueMessage;
        _messageContext = messageContext;
    }

    public void notifyMessageProcessed() {
        _messageContext.onMessageHandled();
    }
}
