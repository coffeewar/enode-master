package com.qianzhui.enode.jmx;

/**
 * Created by xujunbo on 17-9-15.
 */
public interface ForkJoinPoolMBean {
    int getQueuedSubmissionCount();
    int getActiveThreadCount();
    int getCommonPoolParallelism();
    int getPoolSize();
    long getQueuedTaskCount();
    int getRunningThreadCount();
    long getStealCount();
}
