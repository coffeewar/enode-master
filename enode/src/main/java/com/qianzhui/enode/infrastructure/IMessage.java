package com.qianzhui.enode.infrastructure;

import java.util.Date;

/**
 * Created by junbo_xu on 2016/2/24.
 */
public interface IMessage {
    String id();

    void setId(String id);

    Date timestamp();

    void setTimestamp(Date timestamp);

    int sequence();

    void setSequence(int sequence);

    String getRoutingKey();

    String getTypeName();
}
