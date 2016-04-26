package com.qianzhui.enode.common.socketing.buffermanagement;

/**
 * Created by junbo_xu on 2016/3/4.
 */
public interface IPool<T> {
    int getTotalCount();

    int getAvailableCount();

    boolean shrink();

    T get();

    void returns(T item);
}
