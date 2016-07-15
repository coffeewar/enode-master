package com.qianzhui.enode.rocketmq.domainevent;

import com.alibaba.rocketmq.common.message.MessageExt;
import com.qianzhui.enode.common.container.GenericTypeLiteral;
import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.rocketmq.consumer.listener.CompletableConsumeConcurrentlyContext;
import com.qianzhui.enode.common.serializing.IJsonSerializer;
import com.qianzhui.enode.common.utilities.BitConverter;
import com.qianzhui.enode.eventing.DomainEventStreamMessage;
import com.qianzhui.enode.eventing.IDomainEvent;
import com.qianzhui.enode.eventing.IEventSerializer;
import com.qianzhui.enode.infrastructure.IMessageProcessor;
import com.qianzhui.enode.infrastructure.ITypeNameProvider;
import com.qianzhui.enode.infrastructure.ProcessingDomainEventStreamMessage;
import com.qianzhui.enode.rocketmq.*;

import javax.inject.Inject;

/**
 * Created by junbo_xu on 2016/4/6.
 */
public class DomainEventConsumer {
    private final RocketMQConsumer _consumer;
    private final SendReplyService _sendReplyService;
    private final IJsonSerializer _jsonSerializer;
    private final IEventSerializer _eventSerializer;
    private ITypeNameProvider _typeNameProvider;
    private final IMessageProcessor<ProcessingDomainEventStreamMessage, DomainEventStreamMessage, Boolean> _processor;
    private final ILogger _logger;
    private final boolean _sendEventHandledMessage;
    private final ITopicProvider<IDomainEvent> _eventTopicProvider;

    @Inject
    public DomainEventConsumer(RocketMQConsumer rocketMQConsumer, IJsonSerializer jsonSerializer, ITypeNameProvider typeNameProvider,
                               IEventSerializer eventSerializer, IMessageProcessor<ProcessingDomainEventStreamMessage, DomainEventStreamMessage, Boolean> processor,
                               ITopicProvider<IDomainEvent> eventITopicProvider,
                               ILoggerFactory loggerFactory) {
        _consumer = rocketMQConsumer;
        _sendReplyService = new SendReplyService();
        _jsonSerializer = jsonSerializer;
        _typeNameProvider = typeNameProvider;
        _eventSerializer = eventSerializer;
        _processor = processor;
        _logger = loggerFactory.create(getClass());
        _sendEventHandledMessage = true;
        _eventTopicProvider = eventITopicProvider;
    }

    public DomainEventConsumer(RocketMQConsumer consumer, boolean sendEventHandledMessage) {
        _consumer = consumer;
        _sendReplyService = new SendReplyService();
        _jsonSerializer = ObjectContainer.resolve(IJsonSerializer.class);
        _typeNameProvider = ObjectContainer.resolve(ITypeNameProvider.class);
        _eventSerializer = ObjectContainer.resolve(IEventSerializer.class);
        _processor = ObjectContainer.resolve(new GenericTypeLiteral<IMessageProcessor<ProcessingDomainEventStreamMessage, DomainEventStreamMessage, Boolean>>() {
        });
        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(this.getClass());
        _sendEventHandledMessage = sendEventHandledMessage;
        _eventTopicProvider = ObjectContainer.resolve(new GenericTypeLiteral<ITopicProvider<IDomainEvent>>() {
        });
    }

    public DomainEventConsumer start() {
        _consumer.registerMessageHandler(new RocketMQMessageHandler() {
            @Override
            public boolean isMatched(TopicTagData topicTagData) {
                return _eventTopicProvider.getAllSubscribeTopics().contains(topicTagData);
            }

            @Override
            public void handle(MessageExt message, CompletableConsumeConcurrentlyContext context) {
                DomainEventConsumer.this.handle(message, context);
            }
        });

        if (_sendEventHandledMessage) {
            _sendReplyService.start();
        }
        return this;
    }

    public DomainEventConsumer shutdown() {
        if (_sendEventHandledMessage) {
            _sendReplyService.stop();
        }
        return this;
    }

    void handle(final MessageExt msg,
                final CompletableConsumeConcurrentlyContext context) {
        EventStreamMessage message = _jsonSerializer.deserialize(BitConverter.toString(msg.getBody()), EventStreamMessage.class);

        DomainEventStreamMessage domainEventStreamMessage = convertToDomainEventStream(message);
        DomainEventStreamProcessContext processContext = new DomainEventStreamProcessContext(this, domainEventStreamMessage, msg, context);
        ProcessingDomainEventStreamMessage processingMessage = new ProcessingDomainEventStreamMessage(domainEventStreamMessage, processContext);
        _processor.process(processingMessage);
    }

    private DomainEventStreamMessage convertToDomainEventStream(EventStreamMessage message) {
        DomainEventStreamMessage domainEventStreamMessage = new DomainEventStreamMessage(
                message.getCommandId(),
                message.getAggregateRootId(),
                message.getVersion(),
                message.getAggregateRootTypeName(),
                _eventSerializer.deserialize(message.getEvents(), IDomainEvent.class),
                message.getItems()
        );
        domainEventStreamMessage.setTimestamp(message.getTimestamp());

        return domainEventStreamMessage;
    }

    class DomainEventStreamProcessContext extends RocketMQProcessContext {
        private final DomainEventConsumer _eventConsumer;
        private final DomainEventStreamMessage _domainEventStreamMessage;

        public DomainEventStreamProcessContext(DomainEventConsumer eventConsumer, DomainEventStreamMessage domainEventStreamMessage,
                                               MessageExt queueMessage, CompletableConsumeConcurrentlyContext messageContext) {
            super(queueMessage, messageContext);
            _eventConsumer = eventConsumer;
            _domainEventStreamMessage = domainEventStreamMessage;
        }

        @Override
        public void notifyMessageProcessed() {
            super.notifyMessageProcessed();

            if (!_eventConsumer._sendEventHandledMessage) {
                return;
            }

            String replyAddress = _domainEventStreamMessage.getItems().get("CommandReplyAddress");
            if (replyAddress == null || replyAddress.trim().equals("")) {
                return;
            }

            String commandResult = _domainEventStreamMessage.getItems().get("CommandResult");

            DomainEventHandledMessage domainEventHandledMessage = new DomainEventHandledMessage();
            domainEventHandledMessage.setCommandId(_domainEventStreamMessage.getCommandId());
            domainEventHandledMessage.setAggregateRootId(_domainEventStreamMessage.aggregateRootId());
            domainEventHandledMessage.setCommandResult(commandResult);

            _eventConsumer._sendReplyService.sendReply(CommandReplyType.DomainEventHandled.getValue(), domainEventHandledMessage, replyAddress);
        }
    }
}
