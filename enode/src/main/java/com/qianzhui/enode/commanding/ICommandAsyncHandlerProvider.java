package com.qianzhui.enode.commanding;

import com.qianzhui.enode.infrastructure.MessageHandlerData;

import java.util.List;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public interface ICommandAsyncHandlerProvider {
    List<MessageHandlerData<ICommandAsyncHandlerProxy>> getHandlers(Class commandType);
}
