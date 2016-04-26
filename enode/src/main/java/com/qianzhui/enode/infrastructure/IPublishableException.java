package com.qianzhui.enode.infrastructure;

import java.util.Map;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public interface IPublishableException extends IMessage {
    void serializeTo(Map<String, String> serializableInfo);

    void restoreFrom(Map<String, String> serializableInfo);
}
