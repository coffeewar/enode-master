package com.qianzhui.enode.infrastructure;

import com.qianzhui.enode.common.utilities.Ensure;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public class ProcessingMessageMailbox<X extends IProcessingMessage<X, Y, Z>, Y extends IMessage, Z> {
    private ConcurrentMap<Integer, X> _waitingMessageDict;
    private final ConcurrentLinkedQueue<X> _messageQueue;
    private final IProcessingMessageScheduler<X, Y, Z> _scheduler;
    private final IProcessingMessageHandler<X, Y, Z> _messageHandler;
    private AtomicBoolean _isHandlingMessage = new AtomicBoolean(false);
    private final Object _lockObj = new Object();

    public ProcessingMessageMailbox(IProcessingMessageScheduler<X, Y, Z> scheduler, IProcessingMessageHandler<X, Y, Z> messageHandler) {
        _messageQueue = new ConcurrentLinkedQueue<>();
        _scheduler = scheduler;
        _messageHandler = messageHandler;
    }

    public void enqueueMessage(X processingMessage) {
        processingMessage.setMailbox(this);

        _messageQueue.add(processingMessage);
    }

    public boolean enterHandlingMessage() {
        return _isHandlingMessage.compareAndSet(false, true);
    }

    public void exitHandlingMessage() {
        _isHandlingMessage.set(false);
    }

    public void addWaitingForRetryMessage(X waitingMessage) {
        if(!(waitingMessage.getMessage() instanceof ISequenceMessage)){
            throw new IllegalArgumentException("sequenceMessage should not be null.");
        }

        ISequenceMessage sequenceMessage = (ISequenceMessage) waitingMessage.getMessage();

        if (_waitingMessageDict == null)
        {
            synchronized (_lockObj)
            {
                if (_waitingMessageDict == null)
                {
                    _waitingMessageDict = new ConcurrentHashMap<>();
                }
            }
        }

        _waitingMessageDict.putIfAbsent(sequenceMessage.version(), waitingMessage);

        exitHandlingMessage();
        registerForExecution();
    }

    public void completeMessage(X processingMessage) {
        if (!tryExecuteWaitingMessage(processingMessage)) {
            exitHandlingMessage();
            registerForExecution();
        }
    }

    public void run() {
        X processingMessage = null;
        try {
            processingMessage = _messageQueue.poll();

            if (processingMessage != null) {
                _messageHandler.handleAsync(processingMessage);
            }
        } finally {
            if (processingMessage == null) {
                exitHandlingMessage();
                if (!_messageQueue.isEmpty()) {
                    registerForExecution();
                }
            }
        }
    }

    private boolean tryExecuteWaitingMessage(X currentCompletedMessage) {
        if(!(currentCompletedMessage.getMessage() instanceof ISequenceMessage))
            return false;

        ISequenceMessage sequenceMessage = (ISequenceMessage) currentCompletedMessage.getMessage();
        if (sequenceMessage == null) return false;

        if(_waitingMessageDict == null)
            return false;

        X nextMessage = _waitingMessageDict.remove(sequenceMessage.version() + 1);

        if (nextMessage != null) {
            _scheduler.scheduleMessage(nextMessage);
            return true;
        }
        return false;
    }

    private void registerForExecution() {
        _scheduler.scheduleMailbox(this);
    }
}
