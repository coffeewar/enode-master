package com.qianzhui.enode.common.remoting;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public class RemotingResponse extends RemotingMessage {
    private short requestCode;

    public RemotingResponse(short requestCode, short responseCode, short requestType, byte[] responseBody, long requestSequence) {
        super(responseCode, responseBody, requestSequence);
        this.requestCode = requestCode;
        setType(requestType);
    }

    @Override
    public String toString() {
        return String.format("[RequestCode:%d, ResponseCode:%d, RequestType:%d, RequestSequence:%d]", getRequestCode(), getCode(), getType(), getSequence());
    }

    public short getRequestCode() {
        return requestCode;
    }
}
