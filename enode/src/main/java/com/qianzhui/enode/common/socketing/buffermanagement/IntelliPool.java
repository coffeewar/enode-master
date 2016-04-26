package com.qianzhui.enode.common.socketing.buffermanagement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by junbo_xu on 2016/3/7.
 */
public class IntelliPool<T> extends IntelliPoolBase<T> {
    private Map<T, PoolItemState> _bufferDict = new ConcurrentHashMap<>();
    private Map<T, T> _removedItemDict;

    public IntelliPool(int initialCount, IPoolItemCreator<T> itemCreator, Consumer<T> itemCleaner, Consumer<T> itemPreGet) {
        super(initialCount, itemCreator, itemCleaner, itemPreGet);
    }

    @Override
    protected void registerNewItem(T item) {
        PoolItemState state = new PoolItemState();
        state.setGeneration(getCurrentGeneration());
        _bufferDict.put(item, state);
    }

    @Override
    public boolean shrink() {
        byte generation = getCurrentGeneration();

        if (!super.shrink())
            return false;

        List<T> toBeRemoved = new ArrayList<>(getTotalCount() / 2);

        _bufferDict.forEach((k, v) -> {
            if (v.getGeneration() == generation) {
                toBeRemoved.add(k);
            }
        });

        if (_removedItemDict == null)
            _removedItemDict = new ConcurrentHashMap<>();

        toBeRemoved.forEach(item -> {
            PoolItemState state = _bufferDict.remove(item);

            if (state != null)
                _removedItemDict.putIfAbsent(item, item);
        });

        return true;
    }

    @Override
    protected boolean canReturn(T item) {
        return _bufferDict.containsKey(item);
    }

    @Override
    protected boolean tryRemove(T item) {
        if (_removedItemDict == null || _removedItemDict.size() == 0)
            return false;

        T remove = _removedItemDict.remove(item);

        return remove != null;
    }
}
