package com.qianzhui.enode.rocketmq.command;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.qianzhui.enode.commanding.*;
import com.qianzhui.enode.common.container.GenericTypeLiteral;
import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.serializing.IJsonSerializer;
import com.qianzhui.enode.common.utilities.BitConverter;
import com.qianzhui.enode.domain.IAggregateRoot;
import com.qianzhui.enode.domain.IAggregateStorage;
import com.qianzhui.enode.domain.IRepository;
import com.qianzhui.enode.infrastructure.IApplicationMessage;
import com.qianzhui.enode.infrastructure.ITypeNameProvider;
import com.qianzhui.enode.rocketmq.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by junbo_xu on 2016/3/13.
 */
public class CommandConsumer {
    private final RocketMQConsumer _consumer;
    private final SendReplyService _sendReplyService;
    private final IJsonSerializer _jsonSerializer;
    private final ITypeNameProvider _typeNameProvider;
    private final ICommandProcessor _processor;
    private final IRepository _repository;
    private final IAggregateStorage _aggregateRootStorage;
    private final ITopicProvider<ICommand> _commandTopicProvider;
    private final ILogger _logger;

    public RocketMQConsumer getConsumer() {
        return _consumer;
    }

    public CommandConsumer() {
        _consumer = ObjectContainer.resolve(RocketMQConsumer.class);
        _sendReplyService = new SendReplyService();
        _jsonSerializer = ObjectContainer.resolve(IJsonSerializer.class);
        _typeNameProvider = ObjectContainer.resolve(ITypeNameProvider.class);
        _processor = ObjectContainer.resolve(ICommandProcessor.class);
        _repository = ObjectContainer.resolve(IRepository.class);
        _aggregateRootStorage = ObjectContainer.resolve(IAggregateStorage.class);
        _commandTopicProvider = ObjectContainer.resolve(new GenericTypeLiteral<ITopicProvider<ICommand>>() {
        });
        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(getClass());
    }

    public CommandConsumer start() {
        _consumer.registerMessageHandler(new RocketMQMessageHandler() {
            @Override
            public boolean isMatched(TopicTagData topicTagData) {
                return _commandTopicProvider.getAllSubscribeTopics().contains(topicTagData);
            }

            @Override
            public ConsumeConcurrentlyStatus handle(MessageExt message, ConsumeConcurrentlyContext context) {
                return CommandConsumer.this.handle(message, context);
            }
        });

        _sendReplyService.start();
        return this;
    }

    public CommandConsumer shutdown() {
        _sendReplyService.stop();
        return this;
    }

    //TODO consume ack
    ConsumeConcurrentlyStatus handle(final MessageExt msg,
                                     final ConsumeConcurrentlyContext context) {
        Map<String, String> commandItems = new HashMap<>();
        CommandMessage commandMessage = _jsonSerializer.deserialize(BitConverter.toString(msg.getBody()), CommandMessage.class);
        Class commandType = _typeNameProvider.getType(commandMessage.getCommandType());
        ICommand command = (ICommand) _jsonSerializer.deserialize(commandMessage.getCommandData(), commandType);
        CommandExecuteContext commandExecuteContext = new CommandExecuteContext(_repository, _aggregateRootStorage, msg, /*context, */commandMessage, _sendReplyService);
        commandItems.put("CommandReplyAddress", commandMessage.getReplyAddress());
        _processor.process(new ProcessingCommand(command, commandExecuteContext, commandItems));

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    class CommandExecuteContext implements ICommandExecuteContext {
        private String _result;
        private final ConcurrentMap<String, IAggregateRoot> _trackingAggregateRootDict;
        private final IRepository _repository;
        private final IAggregateStorage _aggregateRootStorage;
        private final SendReplyService _sendReplyService;
        private final MessageExt _queueMessage;
        //        private  IMessageContext _messageContext;
        private CommandMessage _commandMessage;

        public CommandExecuteContext(IRepository repository, IAggregateStorage aggregateRootStorage, MessageExt queueMessage, /*IMessageContext messageContext,*/ CommandMessage commandMessage, SendReplyService sendReplyService) {
            _trackingAggregateRootDict = new ConcurrentHashMap<>();
            _repository = repository;
            _aggregateRootStorage = aggregateRootStorage;
            _sendReplyService = sendReplyService;
            _queueMessage = queueMessage;
            _commandMessage = commandMessage;
//            _messageContext = messageContext;
        }

        @Override
        public void onCommandExecuted(CommandResult commandResult) {
//        _messageContext.OnMessageHandled(_queueMessage);

            if (_commandMessage.getReplyAddress() == null) {
                return;
            }

            _sendReplyService.sendReply(CommandReplyType.CommandExecuted.getValue(), commandResult, _commandMessage.getReplyAddress());
        }

        @Override
        public void add(IAggregateRoot aggregateRoot) {
            if (aggregateRoot == null) {
                throw new NullPointerException("aggregateRoot");
            }

            if (_trackingAggregateRootDict.containsKey(aggregateRoot.uniqueId())) {
                throw new AggregateRootAlreadyExistException(aggregateRoot.uniqueId(), aggregateRoot.getClass());
            }

            _trackingAggregateRootDict.put(aggregateRoot.uniqueId(), aggregateRoot);
        }

        @Override
        public <T extends IAggregateRoot> T get(Class<T> aggregateRootType, Object id) {
            return get(aggregateRootType, id, true);
        }

        @Override
        public <T extends IAggregateRoot> T get(Class<T> aggregateRootType, Object id, boolean firstFromCache) {
            if (id == null) {
                throw new NullPointerException("id");
            }

            String aggregateRootId = id.toString();
            IAggregateRoot aggregateRoot = _trackingAggregateRootDict.get(aggregateRootId);
            if (aggregateRoot != null) {
                return (T) aggregateRoot;
            }

            if (firstFromCache) {
                aggregateRoot = _repository.get(aggregateRootType, id);
            } else {
                aggregateRoot = _aggregateRootStorage.get(aggregateRootType, aggregateRootId);
            }

            if (aggregateRoot != null) {
                _trackingAggregateRootDict.putIfAbsent(aggregateRoot.uniqueId(), aggregateRoot);
                return (T) aggregateRoot;
            }

            return null;
        }

        @Override
        public List<IAggregateRoot> getTrackedAggregateRoots() {
            return new ArrayList<>(_trackingAggregateRootDict.values());
        }

        @Override
        public void clear() {
            _trackingAggregateRootDict.clear();
            _result = null;
        }

        @Override
        public void setResult(String result) {
            _result = result;
        }

        @Override
        public String getResult() {
            return _result;
        }
    }
}
