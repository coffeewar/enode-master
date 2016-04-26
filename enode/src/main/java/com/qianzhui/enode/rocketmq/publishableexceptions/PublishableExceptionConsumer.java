package com.qianzhui.enode.rocketmq.publishableexceptions;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.qianzhui.enode.common.container.GenericTypeLiteral;
import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.serializing.IJsonSerializer;
import com.qianzhui.enode.common.utilities.BitConverter;
import com.qianzhui.enode.infrastructure.*;
import com.qianzhui.enode.rocketmq.RocketMQConsumer;
import com.qianzhui.enode.rocketmq.RocketMQMessageHandler;
import com.qianzhui.enode.rocketmq.RocketMQProcessContext;

import javax.inject.Inject;


/**
 * Created by junbo_xu on 2016/4/6.
 */
public class PublishableExceptionConsumer {
    private final RocketMQConsumer _consumer;
    private final IJsonSerializer _jsonSerializer;
    private final ITypeNameProvider _typeNameProvider;
    private final IMessageProcessor<ProcessingPublishableExceptionMessage, IPublishableException, Boolean> _publishableExceptionProcessor;
    private final ILogger _logger;

    @Inject
    public PublishableExceptionConsumer(RocketMQConsumer consumer, IJsonSerializer jsonSerializer, ITypeNameProvider typeNameProvider,
                                        IMessageProcessor<ProcessingPublishableExceptionMessage, IPublishableException, Boolean> publishableExceptionProcessor,
                                        ILoggerFactory loggerFactory) {
        _consumer = consumer;
        _jsonSerializer = jsonSerializer;
        _typeNameProvider = typeNameProvider;
        _publishableExceptionProcessor = publishableExceptionProcessor;
        _logger = loggerFactory.create(getClass());
    }

    public PublishableExceptionConsumer(RocketMQConsumer consumer) {
        _consumer = consumer;
        _jsonSerializer = ObjectContainer.resolve(IJsonSerializer.class);
        _publishableExceptionProcessor = ObjectContainer.resolve(new GenericTypeLiteral<IMessageProcessor<ProcessingPublishableExceptionMessage, IPublishableException, Boolean>>() {
        });
        _typeNameProvider = ObjectContainer.resolve(ITypeNameProvider.class);
        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(getClass());
    }

    public PublishableExceptionConsumer start() {
        _consumer.registerMessageHandler(new RocketMQMessageHandler() {
            @Override
            public boolean isMatched(String messageTags) {
                try {
                    Class type = _typeNameProvider.getType(messageTags);
                    return IPublishableException.class.isAssignableFrom(type);
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public ConsumeConcurrentlyStatus handle(MessageExt message, ConsumeConcurrentlyContext context) {
                return PublishableExceptionConsumer.this.handle(message, context);
            }
        });
        return this;
    }

    public PublishableExceptionConsumer shutdown() {
        return this;
    }

    ConsumeConcurrentlyStatus handle(final MessageExt msg, final ConsumeConcurrentlyContext context) {
        PublishableExceptionMessage exceptionMessage = _jsonSerializer.deserialize(BitConverter.toString(msg.getBody()), PublishableExceptionMessage.class);
        Class exceptionType = _typeNameProvider.getType(msg.getTags());

        IPublishableException exception = null;

        try {
            exception = (IPublishableException) exceptionType.getConstructor().newInstance();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
        exception.setId(exceptionMessage.getUniqueId());
        exception.setTimestamp(exceptionMessage.getTimestamp());
        exception.restoreFrom(exceptionMessage.getSerializableInfo());

        ISequenceMessage sequenceMessage = (ISequenceMessage) exception;
        sequenceMessage.setAggregateRootTypeName(exceptionMessage.getAggregateRootTypeName());
        sequenceMessage.setAggregateRootStringId(exceptionMessage.getAggregateRootId());

        RocketMQProcessContext processContext = new RocketMQProcessContext(msg, context);
        ProcessingPublishableExceptionMessage processingMessage = new ProcessingPublishableExceptionMessage(exception, processContext);
        _publishableExceptionProcessor.process(processingMessage);

        //TODO consume status ack
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    public RocketMQConsumer getConsumer() {
        return _consumer;
    }
}
