package com.qianzhui.enode.eventing;

import com.qianzhui.enode.common.extensions.IEnum;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public enum EventAppendResult implements IEnum {
    Success(1),
    Failed(2),
    DuplicateEvent(3),
    DuplicateCommand(4);

    private int _status;

    EventAppendResult(int status) {
        _status = status;
    }

    @Override
    public int status() {
        return _status;
    }
}
