package com.qianzhui.enode.common.remoting;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public class RemotingRequest extends RemotingMessage {
    private static AtomicLong _sequence = new AtomicLong();

    public RemotingRequest(short code, byte[] body) {
        super(code, body, _sequence.incrementAndGet());
    }

    public RemotingRequest(short code, byte[] body, long sequence) {
        super(code, body, _sequence.incrementAndGet());
    }

    public RemotingRequest(short code, byte[] body, short type, long sequence) {
        super(code, body, type, _sequence.incrementAndGet());
    }

    @Override
    public String toString() {
        return String.format("[Code:%d, Type:%d, Sequence:%d]", getCode(), getType(), getSequence());
    }
}
