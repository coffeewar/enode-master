package com.qianzhui.enode.commanding;

import com.qianzhui.enode.domain.IAggregateRoot;

import java.util.List;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public interface ITrackingContext {
    List<IAggregateRoot> getTrackedAggregateRoots();

    void clear();
}
