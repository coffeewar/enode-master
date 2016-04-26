package com.qianzhui.enode.eventing.impl;

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
    private final String _aggregateRootId;
    private final Queue<EventCommittingContext> _messageQueue;
    private final Consumer<List<EventCommittingContext>> _handleMessageAction;
    private AtomicBoolean _isHandlingMessage;
    private int _batchSize;

    public String getAggregateRootId() {
        return _aggregateRootId;
    }

    public EventMailBox(String aggregateRootId, int batchSize, Consumer<List<EventCommittingContext>> handleMessageAction) {
        _aggregateRootId = aggregateRootId;
        _messageQueue = new ConcurrentLinkedQueue<>();
        _batchSize = batchSize;
        _handleMessageAction = handleMessageAction;
        _isHandlingMessage = new AtomicBoolean(false);
    }

    public void enqueueMessage(EventCommittingContext message) {
        _messageQueue.add(message);
        registerForExecution(false);
    }

    public void clear() {
        _messageQueue.clear();
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
        } finally {
            if (contextList == null || contextList.size() == 0) {
                exitHandlingMessage();
                if (!_messageQueue.isEmpty()) {
                    registerForExecution(false);
                }
            }
        }
    }

    public void exitHandlingMessage() {
        _isHandlingMessage.set(false);
    }

    public void registerForExecution(boolean exitFirst) {
        if (exitFirst) {
            exitHandlingMessage();
        }
        if (enterHandlingMessage()) {
            CompletableFuture.runAsync(this::run);
        }
    }

    private boolean enterHandlingMessage() {
        return _isHandlingMessage.compareAndSet(false, true);
    }
}
