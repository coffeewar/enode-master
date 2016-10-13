package com.qianzhui.enode.commanding;

import com.qianzhui.enode.common.io.IORuntimeException;
import com.qianzhui.enode.common.logging.ILogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by junbo_xu on 2016/4/22.
 */
public class ProcessingCommandMailbox {
    private final ILogger _logger;
    private final Object _lockObj = new Object();
    private final Object _lockObj2 = new Object();
    private final String _aggregateRootId;
    private final ConcurrentMap<Long, ProcessingCommand> _messageDict;
    private final Map<Long, CommandResult> _requestToCompleteOffsetDict;
    private final IProcessingCommandScheduler _scheduler;
    private final IProcessingCommandHandler _messageHandler;
    private long _maxOffset;
    private long _consumingOffset;
    private long _consumedOffset;
    private AtomicBoolean _isHandlingMessage;
    private int _stopHandling;

    public String getAggregateRootId() {
        return _aggregateRootId;
    }

    public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandScheduler scheduler, IProcessingCommandHandler messageHandler, ILogger logger) {
        _messageDict = new ConcurrentHashMap<>();
        _requestToCompleteOffsetDict = new HashMap<>();
        _aggregateRootId = aggregateRootId;
        _scheduler = scheduler;
        _messageHandler = messageHandler;
        _logger = logger;
        _consumedOffset = -1;
        _isHandlingMessage = new AtomicBoolean(false);
    }

    public void enqueueMessage(ProcessingCommand message) {
        //TODO synchronized
        synchronized (_lockObj) {
            message.setSequence(_maxOffset);
            message.setMailbox(this);
            _messageDict.putIfAbsent(message.getSequence(), message);
            _maxOffset++;
        }
        registerForExecution();
    }

    public boolean enterHandlingMessage() {
        return _isHandlingMessage.compareAndSet(false, true);
    }

    public void stopHandlingMessage() {
        _stopHandling = 1;
    }

    public void resetConsumingOffset(long consumingOffset) {
        _consumingOffset = consumingOffset;
    }

    public void restartHandlingMessage() {
        _stopHandling = 0;
        tryExecuteNextMessage();
    }

    public void tryExecuteNextMessage() {
        exitHandlingMessage();
        registerForExecution();
    }

    public void completeMessage(ProcessingCommand message, CommandResult commandResult) {
        //TODO synchronized
        synchronized (_lockObj2) {
            if (message.getSequence() == _consumedOffset + 1) {
                _messageDict.remove(message.getSequence());
                _consumedOffset = message.getSequence();
                completeMessageWithResult(message, commandResult);
                processRequestToCompleteOffsets();
            } else if (message.getSequence() > _consumedOffset + 1) {
                _requestToCompleteOffsetDict.put(message.getSequence(), commandResult);
            } else if (message.getSequence() < _consumedOffset + 1) {
                _messageDict.remove(message.getSequence());
                _requestToCompleteOffsetDict.remove(message.getSequence());
            }
        }
    }

    public void run() {
        if (_stopHandling == 1) {
            return;
        }
        boolean hasException = false;
        ProcessingCommand processingMessage = null;
        try {
            if (hasRemainningMessage()) {
                processingMessage = getNextMessage();
                increaseConsumingOffset();

                if (processingMessage != null) {
                    _messageHandler.handleAsync(processingMessage);
                }
            }
        } catch (Throwable ex) {
            hasException = true;

            if (ex instanceof IOException || ex instanceof IORuntimeException) {
                //We need to retry the command.
                decreaseConsumingOffset();
            }

            if (processingMessage != null) {
                ICommand command = processingMessage.getMessage();
                _logger.error(String.format("Failed to handle command [id: %s, type: %s]", command.id(), command.getClass().getName()), ex);
            } else {
                _logger.error("Failed to run command mailbox.", ex);
            }
        } finally {
            if (hasException || processingMessage == null) {
                exitHandlingMessage();
                if (hasRemainningMessage()) {
                    registerForExecution();
                }
            }
        }
    }

    private void processRequestToCompleteOffsets() {
        long nextSequence = _consumedOffset + 1;

        while (_requestToCompleteOffsetDict.containsKey(nextSequence)) {
            ProcessingCommand processingCommand = _messageDict.remove(nextSequence);

            if (processingCommand != null) {
                completeMessageWithResult(processingCommand, _requestToCompleteOffsetDict.get(nextSequence));
            }
            _requestToCompleteOffsetDict.remove(nextSequence);
            _consumedOffset = nextSequence;

            nextSequence++;
        }
    }

    private void completeMessageWithResult(ProcessingCommand processingCommand, CommandResult commandResult) {
        try {
            processingCommand.complete(commandResult);
        } catch (Exception ex) {
            _logger.error(String.format("Failed to complete command, commandId: %s, aggregateRootId: %s", processingCommand.getMessage().id(), processingCommand.getMessage().getAggregateRootId()), ex);
        }
    }

    private void exitHandlingMessage() {
        _isHandlingMessage.set(false);
    }

    private boolean hasRemainningMessage() {
        return _consumingOffset < _maxOffset;
    }

    private ProcessingCommand getNextMessage() {
        return _messageDict.get(_consumingOffset);
    }

    private void increaseConsumingOffset() {
        _consumingOffset++;
    }

    private void decreaseConsumingOffset() {
        _consumingOffset--;
    }

    private void registerForExecution() {
        _scheduler.scheduleMailbox(this);
    }
}
