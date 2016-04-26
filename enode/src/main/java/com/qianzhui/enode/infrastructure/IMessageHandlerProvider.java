package com.qianzhui.enode.infrastructure;

import java.util.List;

/**
 * Created by junbo_xu on 2016/3/31.
 */
public interface IMessageHandlerProvider {
    List<MessageHandlerData<IMessageHandlerProxy1>> getHandlers(Class messageType);
}
