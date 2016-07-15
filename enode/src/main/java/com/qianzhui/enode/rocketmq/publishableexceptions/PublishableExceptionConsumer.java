package com.qianzhui.enode.rocketmq.publishableexceptions;

import com.alibaba.rocketmq.common.message.MessageExt;
import com.qianzhui.enode.common.container.GenericTypeLiteral;
import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.rocketmq.consumer.listener.CompletableConsumeConcurrentlyContext;
import com.qianzhui.enode.common.serializing.IJsonSerializer;
import com.qianzhui.enode.common.utilities.BitConverter;
import com.qianzhui.enode.infrastructure.*;
import com.qianzhui.enode.rocketmq.*;

import javax.inject.Inject;


/**
 * Created by junbo_xu on 2016/4/6.
 */
public class PublishableExceptionConsumer {
    private final RocketMQConsumer _consumer;
    private final IJsonSerializer _jsonSerializer;
    private final ITopicProvider<IPublishableException> _exceptionTopicProvider;
    private final ITypeNameProvider _typeNameProvider;
    private final IMessageProcessor<ProcessingPublishableExceptionMessage, IPublishableException, Boolean> _publishableExceptionProcessor;
    private final ILogger _logger;

    @Inject
    public PublishableExceptionConsumer(RocketMQConsumer consumer, IJsonSerializer jsonSerializer,
                                        ITopicProvider<IPublishableException> exceptionITopicProvider, ITypeNameProvider typeNameProvider,
                                        IMessageProcessor<ProcessingPublishableExceptionMessage, IPublishableException, Boolean> publishableExceptionProcessor,
                                        ILoggerFactory loggerFactory) {
        _consumer = consumer;
        _jsonSerializer = jsonSerializer;
        _exceptionTopicProvider = exceptionITopicProvider;
        _typeNameProvider = typeNameProvider;
        _publishableExceptionProcessor = publishableExceptionProcessor;
        _logger = loggerFactory.create(getClass());
    }

    public PublishableExceptionConsumer(RocketMQConsumer consumer) {
        _consumer = consumer;
        _jsonSerializer = ObjectContainer.resolve(IJsonSerializer.class);
        _exceptionTopicProvider = ObjectContainer.resolve(new GenericTypeLiteral<ITopicProvider<IPublishableException>>() {
        });
        _publishableExceptionProcessor = ObjectContainer.resolve(new GenericTypeLiteral<IMessageProcessor<ProcessingPublishableExceptionMessage, IPublishableException, Boolean>>() {
        });
        _typeNameProvider = ObjectContainer.resolve(ITypeNameProvider.class);
        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(getClass());
    }

    public PublishableExceptionConsumer start() {
        _consumer.registerMessageHandler(new RocketMQMessageHandler() {
            @Override
            public boolean isMatched(TopicTagData topicTagData) {
                return _exceptionTopicProvider.getAllSubscribeTopics().contains(topicTagData);
            }

            @Override
            public void handle(MessageExt message, CompletableConsumeConcurrentlyContext context) {
                PublishableExceptionConsumer.this.handle(message, context);
            }
        });
        return this;
    }

    public PublishableExceptionConsumer shutdown() {
        return this;
    }

    void handle(final MessageExt msg, final CompletableConsumeConcurrentlyContext context) {
        PublishableExceptionMessage exceptionMessage = _jsonSerializer.deserialize(BitConverter.toString(msg.getBody()), PublishableExceptionMessage.class);
        Class exceptionType = _typeNameProvider.getType(exceptionMessage.getExceptionType());

        IPublishableException exception;

        try {
            exception = (IPublishableException) exceptionType.getConstructor().newInstance();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
        exception.setId(exceptionMessage.getUniqueId());
        exception.setTimestamp(exceptionMessage.getTimestamp());
        exception.restoreFrom(exceptionMessage.getSerializableInfo());

        if (exception instanceof ISequenceMessage) {
            ISequenceMessage sequenceMessage = (ISequenceMessage) exception;
            sequenceMessage.setAggregateRootTypeName(exceptionMessage.getAggregateRootTypeName());
            sequenceMessage.setAggregateRootStringId(exceptionMessage.getAggregateRootId());
        }

        RocketMQProcessContext processContext = new RocketMQProcessContext(msg, context);
        ProcessingPublishableExceptionMessage processingMessage = new ProcessingPublishableExceptionMessage(exception, processContext);
        _publishableExceptionProcessor.process(processingMessage);
    }

    public RocketMQConsumer getConsumer() {
        return _consumer;
    }
}
