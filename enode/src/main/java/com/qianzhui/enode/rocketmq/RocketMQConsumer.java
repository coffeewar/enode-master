package com.qianzhui.enode.rocketmq;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.rocketmq.client.Consumer;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by junbo_xu on 2016/4/15.
 */
public class RocketMQConsumer {
    private Consumer _consumer;
    private Set<RocketMQMessageHandler> _handlers;
    private Map<TopicTagData, RocketMQMessageHandler> _handlerDict;
    private ILogger _logger;

    @Inject
    public RocketMQConsumer(Consumer consumer) {
        _consumer = consumer;
        _consumer.registerMessageListener(this::handle);
        _handlers = new HashSet<>();
        _handlerDict = new HashMap<>();
        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(getClass());
    }

    public void registerMessageHandler(RocketMQMessageHandler handler) {
        _handlers.add(handler);
    }

    public void subscribe(String topic, String subExpression) {
        _consumer.subscribe(topic, subExpression);
    }

    public void start() {
        _consumer.start();
    }

    public void shutdown() {
        _consumer.shutdown();
    }

    protected ConsumeConcurrentlyStatus handle(final List<MessageExt> msgs,
                                               final ConsumeConcurrentlyContext context) {

        MessageExt msg = msgs.get(0);
        String topic = msg.getTopic();
        String tag = msg.getTags();
        TopicTagData topicTagData = new TopicTagData(topic, tag);

        RocketMQMessageHandler rocketMQMessageHandler = _handlerDict.get(topicTagData);

        if (rocketMQMessageHandler == null) {
            List<RocketMQMessageHandler> handlers = _handlers.stream().filter(handler -> handler.isMatched(topicTagData)).collect(Collectors.toList());
            if (handlers.size() > 1) {
                _logger.error("Duplicate consume handler with {topic:%s,tags:%s}", msg.getTopic(), msg.getTags());
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }

            rocketMQMessageHandler = handlers.get(0);
            _handlerDict.put(topicTagData, rocketMQMessageHandler);
        }

        if (rocketMQMessageHandler == null) {
            _logger.error("No consume handler found with {topic:%s,tags:%s}", msg.getTopic(), msg.getTags());
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        } else {
            return rocketMQMessageHandler.handle(msg, context);
        }
    }
}
