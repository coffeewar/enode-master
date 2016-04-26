package com.qianzhui.enode.infrastructure;

import com.qianzhui.enode.common.function.Action;

/**
 * Created by junbo_xu on 2016/4/6.
 */
public interface ILockService {
    void addLockKey(String lockKey);

    void executeInLock(String lockKey, Action action);
}
