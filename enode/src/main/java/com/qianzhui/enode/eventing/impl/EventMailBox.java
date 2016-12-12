package com.qianzhui.enode.eventing.impl;

import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.eventing.EventCommittingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Created by junbo_xu on 2016/4/23.
 */
public class EventMailBox {
    private final ILogger _logger;
    private final String _aggregateRootId;
    private final Queue<EventCommittingContext> _messageQueue;
    private final Consumer<List<EventCommittingContext>> _handleMessageAction;
    private AtomicBoolean _isRunning;
    private int _batchSize;

    public String getAggregateRootId() {
        return _aggregateRootId;
    }

    public EventMailBox(String aggregateRootId, int batchSize, Consumer<List<EventCommittingContext>> handleMessageAction, ILogger logger) {
        _aggregateRootId = aggregateRootId;
        _messageQueue = new ConcurrentLinkedQueue<>();
        _batchSize = batchSize;
        _handleMessageAction = handleMessageAction;
        _isRunning = new AtomicBoolean(false);
        _logger = logger;
    }

    public void enqueueMessage(EventCommittingContext message) {
        _messageQueue.add(message);
        tryRun(false);
    }

    public void tryRun() {
        tryRun(false);
    }

    public void tryRun(boolean exitFirst) {
        if (exitFirst) {
            exit();
        }
        if (tryEnter()) {
            CompletableFuture.runAsync(this::run);
        }
    }

    public void run() {
        List<EventCommittingContext> contextList = null;
        try {
            EventCommittingContext context = null;

            while ((context = _messageQueue.poll()) != null) {
                context.setEventMailBox(this);
                if (contextList == null) {
                    contextList = new ArrayList<>();
                }
                contextList.add(context);

                if (contextList.size() == _batchSize) {
                    break;
                }
            }
            if (contextList != null && contextList.size() > 0) {
                _handleMessageAction.accept(contextList);
            }
        } catch (Exception ex) {
            _logger.error(String.format("Event mailbox run has unknown exception, aggregateRootId: %s", _aggregateRootId), ex);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                //ignore
                e.printStackTrace();
            }
        } finally {
            if (contextList == null || contextList.size() == 0) {
                exit();
                if (!_messageQueue.isEmpty()) {
                    tryRun();
                }
            }
        }
    }

    public void exit() {
//        _isHandlingMessage.set(false);
//        _isHandlingMessage.compareAndSet(true, false);
        _isRunning.getAndSet(false);
    }


    public void clear() {
        _messageQueue.clear();
    }

    private boolean tryEnter() {
        return _isRunning.compareAndSet(false, true);
    }
}
