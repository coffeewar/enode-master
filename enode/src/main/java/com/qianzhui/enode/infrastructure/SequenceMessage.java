package com.qianzhui.enode.infrastructure;

/**
 * Created by junbo_xu on 2016/2/24.
 */
public abstract class SequenceMessage<TAggregateRootId> extends Message implements ISequenceMessage {

    private TAggregateRootId _aggregateRootId;
    private String _aggregateRootStringId;
    private String _aggregateRootTypeName;
    private int _version;

    public TAggregateRootId aggregateRootId() {
        return _aggregateRootId;
    }

    public void setAggregateRootId(TAggregateRootId aggregateRootId) {
        _aggregateRootId = aggregateRootId;
        _aggregateRootStringId = aggregateRootId.toString();
    }

    public String aggregateRootStringId() {
        return _aggregateRootStringId;
    }

    public void setAggregateRootStringId(String aggregateRootStringId) {
        this._aggregateRootStringId = aggregateRootStringId;
    }

    public String aggregateRootTypeName() {
        return _aggregateRootTypeName;
    }

    public void setAggregateRootTypeName(String aggregateRootTypeName) {
        _aggregateRootTypeName = aggregateRootTypeName;
    }

    public int version() {
        return _version;
    }

    public void setVersion(int version) {
        _version = version;
    }

    @Override
    public String getRoutingKey() {
        return _aggregateRootStringId;
    }
}
