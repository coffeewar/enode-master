package com.qianzhui.enode.common.socketing.framing;

import com.qianzhui.enode.common.extensions.ArraySegment;
import com.qianzhui.enode.common.function.Action1;
import com.qianzhui.enode.common.logging.ENodeLogger;
import com.qianzhui.enode.common.utilities.Ensure;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by junbo_xu on 2016/3/4.
 */
public class LengthPrefixMessageFramer implements IMessageFramer {

    private static final Logger logger = ENodeLogger.getLog();

    private int headerLength = Integer.SIZE / 8;
    private Action1<ArraySegment<Byte>> receivedHandler;

    private Byte[] messageBuffer;
    private int bufferIndex = 0;
    private int headerBytes = 0;
    private int packageLength = 0;

    @Override
    public List<ArraySegment<Byte>> frameData(ArraySegment<Byte> data) {
        int length = data.getCount();
        ArraySegment<Byte> byteArraySegment = new ArraySegment<>(new Byte[]{(byte) length, (byte) (length >> 8), (byte) (length >> 16), (byte) (length >> 24)});

        return Arrays.asList(byteArraySegment, data);
    }

    @Override
    public void unFrameData(Iterable<ArraySegment<Byte>> data) {
        Ensure.notNull(data, "data");

        data.forEach(this::parse);
    }

    @Override
    public void unFrameData(ArraySegment<Byte> data) {
        parse(data);
    }

    @Override
    public void registerMessageArrivedCallback(Action1<ArraySegment<Byte>> handler) {
        Ensure.notNull(handler, "handler");
        receivedHandler = handler;
    }

    private void parse(ArraySegment<Byte> bytes) {
        Byte[] data = bytes.getArray();

        for (int i = bytes.getOffset(), n = bytes.getOffset() + bytes.getCount(); i < n; i++) {
            if (headerBytes < headerLength) {
                packageLength |= (data[i] << (headerBytes * 8)); // little-endian order
                ++headerBytes;
                if (headerBytes == headerLength) {
                    if (packageLength <= 0) {
                        throw new RuntimeException(String.format("Package length (%d) is out of bounds.", packageLength));
                    }
                    messageBuffer = new Byte[packageLength];
                }
            } else {
                int copyCnt = Math.min(bytes.getCount() + bytes.getOffset() - i, packageLength - bufferIndex);
                try {
                    System.arraycopy(bytes.getArray(), i, messageBuffer, bufferIndex, copyCnt);
                } catch (Exception ex) {
                    logger.error(String.format("Parse message buffer failed, _headerLength: %d, _packageLength: %d, _bufferIndex: %d, copyCnt: %d, _messageBuffer is null: %s",
                            headerBytes,
                            packageLength,
                            bufferIndex,
                            copyCnt,
                            messageBuffer == null), ex);
                    throw ex;
                }
                bufferIndex += copyCnt;
                i += copyCnt - 1;

                if (bufferIndex == packageLength) {
                    if (receivedHandler != null) {
                        try {
                            receivedHandler.apply(new ArraySegment<>(messageBuffer, 0, bufferIndex));
                        } catch (Exception ex) {
                            logger.error("Handle received message fail.", ex);
                        }
                    }
                    messageBuffer = null;
                    headerBytes = 0;
                    packageLength = 0;
                    bufferIndex = 0;
                }
            }
        }
    }
}
