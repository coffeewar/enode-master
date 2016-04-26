package com.qianzhui.enode.common.function;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public interface Func<TResult> {
    TResult apply() throws Exception;
}
