package com.qianzhui.enode.rocketmq.command;

import com.qianzhui.enode.commanding.CommandResult;
import com.qianzhui.enode.commanding.CommandReturnType;
import com.qianzhui.enode.commanding.CommandStatus;
import com.qianzhui.enode.commanding.ICommand;
import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.common.io.AsyncTaskStatus;
import com.qianzhui.enode.common.logging.ENodeLogger;
import com.qianzhui.enode.common.remoting.*;
import com.qianzhui.enode.common.scheduling.Worker;
import com.qianzhui.enode.common.serializing.IJsonSerializer;
import com.qianzhui.enode.common.socketing.NettyServerConfig;
import com.qianzhui.enode.common.utilities.BitConverter;
import com.qianzhui.enode.rocketmq.CommandReplyType;
import com.qianzhui.enode.rocketmq.domainevent.DomainEventHandledMessage;
import org.slf4j.Logger;

import java.net.SocketAddress;
import java.util.concurrent.*;

/**
 * Created by junbo_xu on 2016/3/8.
 */
public class CommandResultProcessor implements IRequestHandler {
    private static final Logger _logger = ENodeLogger.getLog();

    private SocketRemotingServer _remotingServer;
    private ConcurrentMap<String, CommandTaskCompletionSource> _commandTaskDict;
    private BlockingQueue<CommandResult> _commandExecutedMessageLocalQueue;
    private BlockingQueue<DomainEventHandledMessage> _domainEventHandledMessageLocalQueue;
    private Worker _commandExecutedMessageWorker;
    private Worker _domainEventHandledMessageWorker;
    private IJsonSerializer _jsonSerializer;
    private boolean _started;

    public SocketAddress _bindingAddress;

    public CommandResultProcessor(int listenPort, IJsonSerializer jsonSerializer) {
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(listenPort);
        _remotingServer = new SocketRemotingServer("CommandResultProcessor.RemotingServer", nettyServerConfig);
        _commandTaskDict = new ConcurrentHashMap<>();
        _commandExecutedMessageLocalQueue = new LinkedBlockingQueue<>();
        _domainEventHandledMessageLocalQueue = new LinkedBlockingQueue<>();
        _commandExecutedMessageWorker = new Worker("ProcessExecutedCommandMessage", () -> processExecutedCommandMessage(_commandExecutedMessageLocalQueue.take()));
        _domainEventHandledMessageWorker = new Worker("ProcessDomainEventHandledMessage", () -> processDomainEventHandledMessage(_domainEventHandledMessageLocalQueue.take()));
        _jsonSerializer = jsonSerializer;
    }

    public void registerProcessingCommand(ICommand command, CommandReturnType commandReturnType, CompletableFuture<AsyncTaskResult<CommandResult>> taskCompletionSource) {
        if (_commandTaskDict.containsKey(command.id())) {
            throw new RuntimeException(String.format("Duplicate processing command registration, type:%s, id:%s", command.getClass().getName(), command.id()));
        }

        _commandTaskDict.put(command.id(), new CommandTaskCompletionSource(commandReturnType, taskCompletionSource));
    }

    public void processFailedSendingCommand(ICommand command) {
        CommandTaskCompletionSource commandTaskCompletionSource = _commandTaskDict.remove(command.id());

        if (commandTaskCompletionSource != null) {
            CommandResult commandResult = new CommandResult(CommandStatus.Failed, command.id(), command.getAggregateRootId(), "Failed to send the command.", String.class.getName());
            commandTaskCompletionSource.getTaskCompletionSource().complete(new AsyncTaskResult<>(AsyncTaskStatus.Success, commandResult));
        }
    }

    public CommandResultProcessor start() {
        if (_started) return this;

        _remotingServer.start();
        _bindingAddress = _remotingServer.getServerSocket().getListeningEndPoint();
        _commandExecutedMessageWorker.start();
        _domainEventHandledMessageWorker.start();

        _remotingServer.registerRequestHandler(CommandReplyType.CommandExecuted.getValue(), this);
        _remotingServer.registerRequestHandler(CommandReplyType.DomainEventHandled.getValue(), this);

        _started = true;

        _logger.info("Command result processor started, bindingAddress: {}", _remotingServer.getServerSocket().getListeningEndPoint());

        return this;
    }

    public CommandResultProcessor shutdown() {
        _remotingServer.shutdown();
        _commandExecutedMessageWorker.stop();
        _domainEventHandledMessageWorker.stop();
        return this;
    }

    public SocketAddress getBindingAddress() {
        return _bindingAddress;
    }

    public RemotingResponse handleRequest(IRequestHandlerContext context, RemotingRequest remotingRequest) {
        if (remotingRequest.getCode() == CommandReplyType.CommandExecuted.ordinal()) {
            String json = BitConverter.toString(remotingRequest.getBody());
            CommandResult result = _jsonSerializer.deserialize(json, CommandResult.class);
            _commandExecutedMessageLocalQueue.add(result);
        } else if (remotingRequest.getCode() == CommandReplyType.DomainEventHandled.ordinal()) {
            String json = BitConverter.toString(remotingRequest.getBody());
            DomainEventHandledMessage message = _jsonSerializer.deserialize(json, DomainEventHandledMessage.class);
            _domainEventHandledMessageLocalQueue.add(message);
        } else {
            _logger.error("Invalid remoting request code: {}", remotingRequest.getCode());
        }
        return null;
    }

    private void processExecutedCommandMessage(CommandResult commandResult) {
        CommandTaskCompletionSource commandTaskCompletionSource = _commandTaskDict.get(commandResult.getCommandId());

        if (commandTaskCompletionSource != null) {
            if (commandTaskCompletionSource.getCommandReturnType().equals(CommandReturnType.CommandExecuted)) {
                _commandTaskDict.remove(commandResult.getCommandId());

                if (commandTaskCompletionSource.getTaskCompletionSource().complete(new AsyncTaskResult<>(AsyncTaskStatus.Success, commandResult))) {
                    _logger.debug("Command result return, {}", commandResult);
                }
            } else if (commandTaskCompletionSource.getCommandReturnType().equals(CommandReturnType.EventHandled)) {
                if (commandResult.getStatus().equals(CommandStatus.Failed) || commandResult.getStatus().equals(CommandStatus.NothingChanged)) {
                    _commandTaskDict.remove(commandResult.getCommandId());
                    if (commandTaskCompletionSource.getTaskCompletionSource().complete(new AsyncTaskResult<>(AsyncTaskStatus.Success, commandResult))) {
                        _logger.debug("Command result return, {}", commandResult);
                    }
                }
            }
        }
    }

    private void processDomainEventHandledMessage(DomainEventHandledMessage message) {
        CommandTaskCompletionSource commandTaskCompletionSource = _commandTaskDict.remove(message.getCommandId());
        if (commandTaskCompletionSource != null) {
            CommandResult commandResult = new CommandResult(CommandStatus.Success, message.getCommandId(), message.getAggregateRootId(), message.getCommandResult(), message.getCommandResult() != null ? String.class.getName() : null);

            if (commandTaskCompletionSource.getTaskCompletionSource().complete(new AsyncTaskResult<>(AsyncTaskStatus.Success, commandResult))) {
                _logger.debug("Command result return, {}", commandResult);
            }
        }
    }

    class CommandTaskCompletionSource {
        private CommandReturnType commandReturnType;
        private CompletableFuture<AsyncTaskResult<CommandResult>> taskCompletionSource;

        public CommandTaskCompletionSource(CommandReturnType commandReturnType, CompletableFuture<AsyncTaskResult<CommandResult>> taskCompletionSource) {
            this.commandReturnType = commandReturnType;
            this.taskCompletionSource = taskCompletionSource;
        }

        public CommandReturnType getCommandReturnType() {
            return commandReturnType;
        }

        public void setCommandReturnType(CommandReturnType commandReturnType) {
            this.commandReturnType = commandReturnType;
        }

        public CompletableFuture<AsyncTaskResult<CommandResult>> getTaskCompletionSource() {
            return taskCompletionSource;
        }

        public void setTaskCompletionSource(CompletableFuture<AsyncTaskResult<CommandResult>> taskCompletionSource) {
            this.taskCompletionSource = taskCompletionSource;
        }
    }
}
