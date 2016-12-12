package com.qianzhui.enode.infrastructure;

import com.qianzhui.enode.common.logging.ILogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public class ProcessingMessageMailbox<X extends IProcessingMessage<X, Y>, Y extends IMessage> {
    private final String _routingKey;
    private final ILogger _logger;
    private ConcurrentMap<Integer, X> _waitingMessageDict;
    private final ConcurrentLinkedQueue<X> _messageQueue;
    private final IProcessingMessageScheduler<X, Y> _scheduler;
    private final IProcessingMessageHandler<X, Y> _messageHandler;
    private AtomicBoolean _isRunning = new AtomicBoolean(false);
    private final Object _lockObj = new Object();

    public ProcessingMessageMailbox(String routingKey, IProcessingMessageScheduler<X, Y> scheduler, IProcessingMessageHandler<X, Y> messageHandler, ILogger logger) {
        _routingKey = routingKey;
        _messageQueue = new ConcurrentLinkedQueue<>();
        _scheduler = scheduler;
        _messageHandler = messageHandler;
        _logger = logger;
    }

    public void enqueueMessage(X processingMessage) {
        processingMessage.setMailbox(this);

        _messageQueue.add(processingMessage);

        tryRun();
    }

    public void addWaitingForRetryMessage(X waitingMessage) {
        if (!(waitingMessage.getMessage() instanceof ISequenceMessage)) {
            throw new IllegalArgumentException("sequenceMessage should not be null.");
        }

        ISequenceMessage sequenceMessage = (ISequenceMessage) waitingMessage.getMessage();

        if (_waitingMessageDict == null) {
            synchronized (_lockObj) {
                if (_waitingMessageDict == null) {
                    _waitingMessageDict = new ConcurrentHashMap<>();
                }
            }
        }

        _waitingMessageDict.putIfAbsent(sequenceMessage.version(), waitingMessage);

        exit();
        tryRun();
    }

    public void completeMessage(X processingMessage) {
        if (!tryExecuteWaitingMessage(processingMessage)) {
            exit();
            tryRun();
        }
    }

    public void run() {
        X processingMessage = null;
        try {
            processingMessage = _messageQueue.poll();

            if (processingMessage != null) {
                _messageHandler.handleAsync(processingMessage);
            }
        } catch (Exception ex) {
            _logger.error(String.format("Message mailbox run has unknown exception, routingKey: %s, commandId: %s", _routingKey, processingMessage != null ? processingMessage.getMessage().id() : ""), ex);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            if (processingMessage == null) {
                exit();
                if (!_messageQueue.isEmpty()) {
                    tryRun();
                }
            }
        }
    }

    private boolean tryExecuteWaitingMessage(X currentCompletedMessage) {
        if (!(currentCompletedMessage.getMessage() instanceof ISequenceMessage))
            return false;

        ISequenceMessage sequenceMessage = (ISequenceMessage) currentCompletedMessage.getMessage();
        if (sequenceMessage == null) return false;

        if (_waitingMessageDict == null)
            return false;

        X nextMessage = _waitingMessageDict.remove(sequenceMessage.version() + 1);

        if (nextMessage != null) {
            _scheduler.scheduleMessage(nextMessage);
            return true;
        }
        return false;
    }

    private void tryRun() {
        if (tryEnter()) {
            _scheduler.scheduleMailbox(this);
        }
    }

    public boolean tryEnter() {
        return _isRunning.compareAndSet(false, true);
    }

    public void exit() {
//        _isHandlingMessage.set(false);
//        _isHandlingMessage.compareAndSet(true, false);
        _isRunning.getAndSet(false);
    }
}
