package com.qianzhui.enode.infrastructure;

import java.util.List;

/**
 * Created by junbo_xu on 2016/4/3.
 */
public interface ITwoMessageHandlerProvider {
    List<MessageHandlerData<IMessageHandlerProxy2>> getHandlers(List<Class> messageTypes);
}
