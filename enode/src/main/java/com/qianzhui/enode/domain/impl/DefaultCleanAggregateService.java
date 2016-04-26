package com.qianzhui.enode.domain.impl;

import com.qianzhui.enode.ENode;
import com.qianzhui.enode.common.scheduling.IScheduleService;
import com.qianzhui.enode.domain.ICleanAggregateService;
import com.qianzhui.enode.domain.IMemoryCache;

import javax.inject.Inject;

/**
 * Created by junbo_xu on 2016/4/23.
 */
public class DefaultCleanAggregateService implements ICleanAggregateService {
    private final IMemoryCache _memoryCache;
    private final IScheduleService _scheduleService;
    private final int TimeoutSeconds;

    @Inject
    public DefaultCleanAggregateService(IMemoryCache memoryCache, IScheduleService scheduleService) {
        TimeoutSeconds = ENode.getInstance().getSetting().getAggregateRootMaxInactiveSeconds();
        _memoryCache = memoryCache;
        _scheduleService = scheduleService;
        _scheduleService.startTask("CleanAggregates", this::clean, 1000, ENode.getInstance().getSetting().getScanExpiredAggregateIntervalMilliseconds());
    }

    public void clean() {
        _memoryCache.getAll().stream().filter(x -> x.isExpired(TimeoutSeconds))
                .forEach(aggregateRootInfo -> _memoryCache.remove(aggregateRootInfo.getAggregateRoot().uniqueId()));
    }
}
