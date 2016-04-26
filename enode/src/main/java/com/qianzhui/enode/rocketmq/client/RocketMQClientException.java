package com.qianzhui.enode.rocketmq.client;

/**
 * Created by junbo_xu on 2016/4/20.
 */
public class RocketMQClientException extends RuntimeException {
    private static final long serialVersionUID = 2779659341449862314L;

    public RocketMQClientException() {
    }


    public RocketMQClientException(String message) {
        super(message);
    }


    public RocketMQClientException(Throwable cause) {
        super(cause);
    }


    public RocketMQClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
