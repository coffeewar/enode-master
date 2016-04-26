package com.qianzhui.enode.infrastructure;

import java.util.Date;
import java.util.UUID;

/**
 * Created by junbo_xu on 2016/2/24.
 */
public abstract class Message implements IMessage {
    private String _id;
    private Date _timestamp;
    private int _sequence;

    public Message() {
        //todo guid
        //id = ObjectId.GenerateNewStringId();
        _id = UUID.randomUUID().toString();
        _timestamp = new Date();
        _sequence = 1;
    }

    public String id() {
        return _id;
    }

    public void setId(String id) {
        _id = id;
    }

    public Date timestamp(){
        return _timestamp;
    }

    public void setTimestamp(Date timeStamp){
        _timestamp=timeStamp;
    }

    public int sequence() {
        return _sequence;
    }

    public void setSequence(int sequence) {
        _sequence = sequence;
    }

    public String getRoutingKey() {
        return null;
    }

    public String getTypeName() {
        return this.getClass().getName();
    }
}
