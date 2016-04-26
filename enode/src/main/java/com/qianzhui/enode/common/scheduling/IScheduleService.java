package com.qianzhui.enode.common.scheduling;

import com.qianzhui.enode.common.function.Action;

/**
 * Created by junbo_xu on 2016/3/11.
 */
public interface IScheduleService {
    void startTask(String name, Action action, int dueTime, int period);

    void stopTask(String name);
}
