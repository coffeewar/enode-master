package com.qianzhui.enode.common.remoting;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public class RemotingMessage {
    private short code;
    private short type;
    private byte[] body;
    private long sequence;

    public RemotingMessage(short code, byte[] body, long sequence) {
        this(code, body, (short) 0, sequence);
    }

    public RemotingMessage(short code, byte[] body, short type, long sequence) {
        this.code = code;
        this.body = body;
        this.type = type;
        this.sequence = sequence;
    }

    public short getCode() {
        return code;
    }

    public short getType() {
        return type;
    }

    protected void setType(short type) {
        this.type = type;
    }

    public byte[] getBody() {
        return body;
    }

    public long getSequence() {
        return sequence;
    }
}
