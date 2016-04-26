package com.qianzhui.enode.infrastructure;

/**
 * Created by junbo_xu on 2016/2/24.
 */
public interface ISequenceMessage extends IMessage {
    String aggregateRootStringId();

    void setAggregateRootStringId(String aggregateRootStringId);

    String aggregateRootTypeName();

    void setAggregateRootTypeName(String aggregateRootTypeName);

    int version();

    void setVersion(int version);
}
