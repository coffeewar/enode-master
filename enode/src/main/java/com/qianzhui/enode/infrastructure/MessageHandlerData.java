package com.qianzhui.enode.infrastructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public class MessageHandlerData<T extends IObjectProxy> {
    public List<T> AllHandlers = new ArrayList<>();
    public List<T> ListHandlers = new ArrayList<>();
    public List<T> QueuedHandlers = new ArrayList<>();
}
