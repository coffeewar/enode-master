package com.qianzhui.enode.rocketmq;

import com.alibaba.rocketmq.client.producer.SendCallback;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.client.producer.SendStatus;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.common.io.AsyncTaskStatus;
import com.qianzhui.enode.common.io.IOHelper;
import com.qianzhui.enode.common.io.IORuntimeException;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.rocketmq.client.Producer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/3/2.
 */
public class SendQueueMessageService {
    private ILogger logger;
    private IOHelper ioHelper;

    public SendQueueMessageService() {
        logger = ObjectContainer.resolve(ILoggerFactory.class).create(SendQueueMessageService.class);
        ioHelper = ObjectContainer.resolve(IOHelper.class);
    }

    public void sendMessage(Producer producer, Message message, String routingKey, String messageId, String version) {
        try {
            ioHelper.tryIOAction(() ->
            {
                SendResult result = producer.send(message, this::messageQueueSelect, routingKey);

                if (!result.getSendStatus().equals(SendStatus.SEND_OK)) {
                    logger.error("ENode message sync send failed, sendResult: %s, routingKey: %s, messageId: %s, version: %s", result, routingKey, messageId, version);
                    throw new IORuntimeException(result.toString());
                }
                logger.info("ENode message sync send success, sendResult: %s, routingKey: %s, messageId: %s, version: %s", result, routingKey, messageId, version);
            }, "SendENodeMessage");
        } catch (Exception ex) {
            logger.error(String.format("ENode message synch send has exception, message: %s, routingKey: %s, messageId: %s, version: %s", message, routingKey, messageId, version), ex);
            throw ex;
        }
    }

    public CompletableFuture<AsyncTaskResult> sendMessageAsync(Producer producer, Message message, String routingKey, String messageId, String version) {
        CompletableFuture<AsyncTaskResult> promise = new CompletableFuture<>();
        try {
            producer.send(message, this::messageQueueSelect, routingKey, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    logger.info("ENode message async send success, sendResult: %s, routingKey: %s, messageId: %s, version: %s", sendResult, routingKey, messageId, version);
                    promise.complete(AsyncTaskResult.Success);
                }

                @Override
                public void onException(Throwable ex) {
                    logger.error("ENode message async send failed, routingKey: %s, messageId: %s, version: %s", routingKey, messageId, version);
                    promise.complete(new AsyncTaskResult(AsyncTaskStatus.IOException, ex.getMessage()));
                }
            });
        } catch (Exception ex) {
            logger.error(String.format("ENode message async send has exception, message: %s, routingKey: %s, messageId: %s, version: %s", message, routingKey, messageId, version), ex);
            promise.complete(new AsyncTaskResult(AsyncTaskStatus.IOException, ex.getMessage()));
        }

        return promise;
    }


    private MessageQueue messageQueueSelect(List<MessageQueue> queues, Message msg, Object routingKey) {
        int hash = Math.abs(routingKey.hashCode());
        return queues.get(hash % queues.size());
    }
}
