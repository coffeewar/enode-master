package com.qianzhui.enode.common.socketing.framing;

import com.qianzhui.enode.common.extensions.ArraySegment;
import com.qianzhui.enode.common.function.Action1;

import java.util.List;

/**
 * Created by junbo_xu on 2016/3/4.
 */
public interface IMessageFramer {
    void unFrameData(Iterable<ArraySegment<Byte>> data);

    void unFrameData(ArraySegment<Byte> data);

    List<ArraySegment<Byte>> frameData(ArraySegment<Byte> data);

    void registerMessageArrivedCallback(Action1<ArraySegment<Byte>> handler);
}
