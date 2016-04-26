package com.qianzhui.enode.common.socketing.buffermanagement;

/**
 * Created by junbo_xu on 2016/3/4.
 */
public interface IBufferPool extends IPool<byte[]> {
    int getBufferSize();
}
