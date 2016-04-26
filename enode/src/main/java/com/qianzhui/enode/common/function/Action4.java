package com.qianzhui.enode.common.function;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public interface Action4<T1, T2, T3, T4> {
    void apply(T1 obj1, T2 obj2, T3 obj3, T4 obj4) throws Exception;
}
