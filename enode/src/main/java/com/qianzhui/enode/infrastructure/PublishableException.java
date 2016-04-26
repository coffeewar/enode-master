package com.qianzhui.enode.infrastructure;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Created by junbo_xu on 2016/4/5.
 */
public abstract class PublishableException extends Exception implements IPublishableException {
    private static final long serialVersionUID = 2099914413380872726L;

    private String id;
    private Date timestamp;
    private int sequence;

    public PublishableException() {
        //TODO ObjectId.GenerateNewStringId();
        //id = ObjectId.GenerateNewStringId();
        id = UUID.randomUUID().toString();
        timestamp = new Date();
        sequence = 1;
    }

    public abstract void serializeTo(Map<String, String> serializableInfo);

    public abstract void restoreFrom(Map<String, String> serializableInfo);

    public String getRoutingKey() {
        return null;
    }

    public String getTypeName() {
        return this.getClass().getName();
    }
}
