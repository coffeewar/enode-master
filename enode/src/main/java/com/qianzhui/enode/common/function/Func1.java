package com.qianzhui.enode.common.function;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public interface Func1<T, TResult> {
    TResult apply(T obj) throws Exception;
}
