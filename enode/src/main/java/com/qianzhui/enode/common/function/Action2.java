package com.qianzhui.enode.common.function;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public interface Action2<T1, T2> {
    void apply(T1 obj1, T2 obj2) throws Exception;
}
