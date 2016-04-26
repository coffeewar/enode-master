package com.qianzhui.enode.common.remoting.exceptions;

/**
 * Created by junbo_xu on 2016/3/11.
 */
public class ResponseFutureAddFailedException extends RuntimeException {
    private static final long serialVersionUID = -5397514307675204065L;

    public ResponseFutureAddFailedException(long requestSequence) {
        super(String.format("Add remoting request response future failed. request sequence:%d", requestSequence));
    }
}
