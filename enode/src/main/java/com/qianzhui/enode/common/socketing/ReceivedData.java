package com.qianzhui.enode.common.socketing;

import com.qianzhui.enode.common.extensions.ArraySegment;

/**
 * Created by junbo_xu on 2016/3/5.
 */
public class ReceivedData {
    private ArraySegment<Byte> Buf;
    private int DataLen;

    public ReceivedData(ArraySegment<Byte> buf, int dataLen) {
        Buf = buf;
        DataLen = dataLen;
    }

    public ArraySegment<Byte> getBuf() {
        return Buf;
    }

    public int getDataLen() {
        return DataLen;
    }
}
