package com.qianzhui.enode.common.socketing.buffermanagement;

import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Created by junbo_xu on 2016/3/6.
 */
public abstract class IntelliPoolBase<T> implements IPool<T> {
    private Stack<T> _store;
    private IPoolItemCreator<T> _itemCreator;
    private byte _currentGeneration = 0;
    private int _nextExpandThreshold;
    private AtomicInteger _totalCount;
    private AtomicInteger _availableCount;
    private AtomicInteger _inExpanding;
    private Consumer<T> _itemCleaner;
    private Consumer<T> _itemPreGet;

    protected byte getCurrentGeneration() {
        return _currentGeneration;
    }

    public int getTotalCount() {
        return _totalCount.get();
    }

    public int getAvailableCount() {
        return _availableCount.get();
    }

    public IntelliPoolBase(int initialCount, IPoolItemCreator<T> itemCreator, Consumer<T> itemCleaner, Consumer<T> itemPreGet) {
        _itemCreator = itemCreator;
        _itemCleaner = itemCleaner;
        _itemPreGet = itemPreGet;

        _store = new Stack<>();

        itemCreator.create(initialCount).forEach(item -> {
            registerNewItem(item);
            _store.push(item);
        });

        _totalCount = new AtomicInteger(initialCount);
        _availableCount = new AtomicInteger(initialCount);
        _inExpanding = new AtomicInteger();
        updateNextExpandThreshold();
    }

    protected abstract void registerNewItem(T item);

    public T get() {
        if (!_store.empty()) {
            T item = _store.pop();

            _availableCount.decrementAndGet();

            if (_availableCount.get() <= _nextExpandThreshold && _inExpanding.get() == 0)
                CompletableFuture.supplyAsync(() -> tryExpand());
//                ThreadPool.QueueUserWorkItem(w = > TryExpand());


            if (_itemPreGet != null)
                    _itemPreGet.accept(item);

            return item;
        }

        //In expanding
        if (_inExpanding.get() == 1) {

            while (true) {
                //spinWait.SpinOnce();
                //TODO
                Thread.yield();

                if (!_store.empty()) {
                    T item = _store.pop();

                    _availableCount.decrementAndGet();

                    if (_itemPreGet != null)
                            _itemPreGet.accept(item);

                    return item;
                }

                if (_inExpanding.get() != 1)
                    return get();
            }
        } else {
            tryExpand();
            return get();
        }
    }

    boolean tryExpand() {

        if (!_inExpanding.compareAndSet(0, 1))
            return false;

        expand();
        _inExpanding.set(0);
        return true;
    }

    AtomicLong _expandCount = new AtomicLong();

    void expand() {
        int totalCount = _totalCount.get();

        _itemCreator.create(totalCount).forEach(item -> {
            _store.push(item);
            _availableCount.incrementAndGet();
            registerNewItem(item);
        });

        long c = _expandCount.incrementAndGet();
        if (c % 1 == 0) {
            System.out.println(String.format("Expand buffer pool, count: %d", c));
        }

        _currentGeneration++;

        _totalCount.addAndGet(totalCount);
        updateNextExpandThreshold();
    }

    public boolean shrink() {
        if (_currentGeneration == 0)
            return false;

        int shrinThreshold = _totalCount.get() * 3 / 4;

        if (_availableCount.get() <= shrinThreshold)
            return false;

        _currentGeneration = (byte) (_currentGeneration - 1);
        return true;
    }

    protected abstract boolean canReturn(T item);

    protected abstract boolean tryRemove(T item);

    public void returns(T item) {
        if (_itemCleaner != null)
                _itemCleaner.accept(item);

        if (canReturn(item)) {
            _store.push(item);
            _availableCount.incrementAndGet();
            return;
        }

        if (tryRemove(item))
            _totalCount.decrementAndGet();
    }

    private void updateNextExpandThreshold() {
        _nextExpandThreshold = _totalCount.get() / 5; //if only 20% buffer left, we can expand the buffer count
    }
}
