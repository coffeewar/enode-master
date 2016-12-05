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
    private final Map<Long, CommandResult> _requestToCompleteSequenceDict;
    private final IProcessingCommandScheduler _scheduler;
    private final IProcessingCommandHandler _messageHandler;
    private long _nextSequence;
    private long _consumingSequence;
    private long _consumedSequence;
    private AtomicBoolean _isHandlingMessage;
    private int _isPaused;

    public String getAggregateRootId() {
        return _aggregateRootId;
    }

    public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandScheduler scheduler, IProcessingCommandHandler messageHandler, ILogger logger) {
        _messageDict = new ConcurrentHashMap<>();
        _requestToCompleteSequenceDict = new HashMap<>();
        _aggregateRootId = aggregateRootId;
        _scheduler = scheduler;
        _messageHandler = messageHandler;
        _logger = logger;
        _consumedSequence = -1;
        _isHandlingMessage = new AtomicBoolean(false);
    }

    public void enqueueMessage(ProcessingCommand message) {
        //TODO synchronized
        synchronized (_lockObj) {
            message.setSequence(_nextSequence);
            message.setMailbox(this);
            ProcessingCommand processingCommand = _messageDict.putIfAbsent(message.getSequence(), message);
            if (processingCommand == message) {
                _nextSequence++;
            }
        }
        registerForExecution();
    }

    public boolean enterHandlingMessage() {
        return _isHandlingMessage.compareAndSet(false, true);
    }

    public void pauseHandlingMessage() {
        _isPaused = 1;
    }

    public void resetConsumingSequence(long consumingSequence) {
        _consumingSequence = consumingSequence;
    }

    public void resumeHandlingMessage() {
        _isPaused = 0;
        //tryExecuteNextMessage();
        registerForExecution();
    }

    public void tryExecuteNextMessage() {
        exitHandlingMessage();
        registerForExecution();
    }

    public void completeMessage(ProcessingCommand processingCommand, CommandResult commandResult) {
        //TODO synchronized
        synchronized (_lockObj2) {
            try {
                if (processingCommand.getSequence() == _consumedSequence + 1) {
                    _messageDict.remove(processingCommand.getSequence());
                    completeCommandWithResult(processingCommand, commandResult);
                    _consumedSequence = processNextCompletedCommands(processingCommand.getSequence());
                } else if (processingCommand.getSequence() > _consumedSequence + 1) {
                    _requestToCompleteSequenceDict.put(processingCommand.getSequence(), commandResult);
                } else if (processingCommand.getSequence() < _consumedSequence + 1) {
                    _messageDict.remove(processingCommand.getSequence());
                    completeCommandWithResult(processingCommand, commandResult);
                    _requestToCompleteSequenceDict.remove(processingCommand.getSequence());
                }
                if (_logger.isDebugEnabled()) {
                    _logger.debug("Command mailbox complete command success, commandId: %s, aggregateRootId: %s", processingCommand.getMessage().id(), processingCommand.getMessage().getAggregateRootId());
                }
            } catch (Exception ex) {
                _logger.error(String.format("Command mailbox complete command failed, commandId: %s, aggregateRootId: %s", processingCommand.getMessage().id(), processingCommand.getMessage().getAggregateRootId()), ex);
            } finally {
                registerForExecution();
            }
        }
    }

    public void run() {
        if (_isPaused == 1) {
            return;
        }
        boolean hasException = false;
        ProcessingCommand processingMessage = null;
        try {
            if (hasRemainningMessage()) {
                processingMessage = getNextMessage();
                increaseConsumingSequence();

                if (processingMessage != null) {
                    _messageHandler.handleAsync(processingMessage);
                }
            }
        } catch (Throwable ex) {
            hasException = true;

            if (ex instanceof IOException || ex instanceof IORuntimeException) {
                //We need to retry the command.
                decreaseConsumingSequence();
            }

            if (processingMessage != null) {
                ICommand command = processingMessage.getMessage();
                _logger.error(String.format("Failed to handle command [id: %s, type: %s], aggregateId: %s", command.id(), command.getClass().getName(), _aggregateRootId), ex);
            } else {
                _logger.error(String.format("Failed to run command mailbox, aggregateId: %s", _aggregateRootId), ex);
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

    private long processNextCompletedCommands(long baseSequence) {
        long returnSequence = baseSequence;
        long nextSequence = baseSequence + 1;

        while (_requestToCompleteSequenceDict.containsKey(nextSequence)) {
            ProcessingCommand processingCommand = _messageDict.remove(nextSequence);

            if (processingCommand != null) {
                completeCommandWithResult(processingCommand, _requestToCompleteSequenceDict.get(nextSequence));
            }
            _requestToCompleteSequenceDict.remove(nextSequence);
            returnSequence = nextSequence;

            nextSequence++;
        }

        return returnSequence;
    }

    private void completeCommandWithResult(ProcessingCommand processingCommand, CommandResult commandResult) {
        try {
            processingCommand.complete(commandResult);
        } catch (Exception ex) {
            _logger.error(String.format("Failed to complete command, commandId: %s, aggregateRootId: %s", processingCommand.getMessage().id(), processingCommand.getMessage().getAggregateRootId()), ex);
        }
    }

    private void exitHandlingMessage() {
//        _isHandlingMessage.compareAndSet(true, false);
        _isHandlingMessage.getAndSet(false);
    }

    private boolean hasRemainningMessage() {
        return _consumingSequence < _nextSequence;
    }

    private ProcessingCommand getNextMessage() {
        return _messageDict.get(_consumingSequence);
    }

    private void increaseConsumingSequence() {
        _consumingSequence++;
    }

    private void decreaseConsumingSequence() {
        _consumingSequence--;
    }

    private void registerForExecution() {
        _scheduler.scheduleMailbox(this);
    }
}
