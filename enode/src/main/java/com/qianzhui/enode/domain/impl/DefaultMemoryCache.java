package com.qianzhui.enode.domain.impl;

import com.qianzhui.enode.ENode;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.scheduling.IScheduleService;
import com.qianzhui.enode.domain.AggregateCacheInfo;
import com.qianzhui.enode.domain.IAggregateRoot;
import com.qianzhui.enode.domain.IAggregateStorage;
import com.qianzhui.enode.domain.IMemoryCache;
import com.qianzhui.enode.infrastructure.ITypeNameProvider;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by junbo_xu on 2016/3/31.
 */
public class DefaultMemoryCache implements IMemoryCache {
    private final ConcurrentMap<String, AggregateCacheInfo> _aggregateRootInfoDict;
    private final IAggregateStorage _aggregateStorage;
    private final ITypeNameProvider _typeNameProvider;
    private final ILogger _logger;
    private final IScheduleService _scheduleService;
    private final int _timeoutSeconds;
    private final String _taskName;

    @Inject
    public DefaultMemoryCache(IScheduleService scheduleService, ITypeNameProvider typeNameProvider, IAggregateStorage aggregateStorage, ILoggerFactory loggerFactory) {
        _scheduleService = scheduleService;
        _aggregateRootInfoDict = new ConcurrentHashMap<>();
        _typeNameProvider = typeNameProvider;
        _aggregateStorage = aggregateStorage;
        _logger = loggerFactory.create(getClass());
        _timeoutSeconds = ENode.getInstance().getSetting().getAggregateRootMaxInactiveSeconds();
        _taskName = "CleanInactiveAggregates_" + System.nanoTime() + new Random().nextInt(10000);
    }

    @Override
    public IAggregateRoot get(Object aggregateRootId, Class aggregateRootType) {
        if (aggregateRootId == null) throw new NullPointerException("aggregateRootId");
        AggregateCacheInfo aggregateRootInfo = _aggregateRootInfoDict.get(aggregateRootId.toString());
        if (aggregateRootInfo != null) {
            IAggregateRoot aggregateRoot = aggregateRootInfo.getAggregateRoot();
            if (aggregateRoot.getClass() != aggregateRootType) {
                throw new RuntimeException(String.format("Incorrect aggregate root type, aggregateRootId:%s, type:%s, expecting type:%s", aggregateRootId, aggregateRoot.getClass(), aggregateRootType));
            }
            if (aggregateRoot.getChanges().size() > 0) {
                IAggregateRoot lastestAggregateRoot = _aggregateStorage.get(aggregateRootType, aggregateRootId.toString());
                if (lastestAggregateRoot != null) {
                    setInternal(lastestAggregateRoot);
                }
                return lastestAggregateRoot;
            }
            return aggregateRoot;
        }
        return null;
    }

    @Override
    public void set(IAggregateRoot aggregateRoot) {
        setInternal(aggregateRoot);
    }

    @Override
    public void refreshAggregateFromEventStore(String aggregateRootTypeName, String aggregateRootId) {
        try {
            Class aggregateRootType = _typeNameProvider.getType(aggregateRootTypeName);
            if (aggregateRootType == null) {
                _logger.error("Could not find aggregate root type by aggregate root type name [%s].", aggregateRootTypeName);
                return;
            }
            IAggregateRoot aggregateRoot = _aggregateStorage.get(aggregateRootType, aggregateRootId);
            if (aggregateRoot != null) {
                setInternal(aggregateRoot);
            }
        } catch (Exception ex) {
            _logger.error(String.format("Refresh aggregate from event store has unknown exception, aggregateRootTypeName:%s, aggregateRootId:%s", aggregateRootTypeName, aggregateRootId), ex);
        }
    }

    @Override
    public void start() {
        _scheduleService.startTask(_taskName, this::cleanInactiveAggregateRoot, ENode.getInstance().getSetting().getScanExpiredAggregateIntervalMilliseconds(), ENode.getInstance().getSetting().getScanExpiredAggregateIntervalMilliseconds());
    }

    @Override
    public void stop() {
        _scheduleService.stopTask(_taskName);
    }

    private void setInternal(IAggregateRoot aggregateRoot) {
        if (aggregateRoot == null) {
            throw new NullPointerException("aggregateRoot");
        }

        _aggregateRootInfoDict.merge(aggregateRoot.uniqueId(), new AggregateCacheInfo(aggregateRoot),
                (oldValue, value) -> {
                    oldValue.setAggregateRoot(aggregateRoot);
                    oldValue.setLastUpdateTimeMillis(System.currentTimeMillis());

                    if (_logger.isDebugEnabled()) {
                        _logger.debug("In memory aggregate updated, type: %s, id: %s, version: %d", aggregateRoot.getClass().getName(), aggregateRoot.uniqueId(), aggregateRoot.version());
                    }

                    return oldValue;
                });
    }

    private void cleanInactiveAggregateRoot() {
        List<Map.Entry<String, AggregateCacheInfo>> inactiveList = _aggregateRootInfoDict.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired(_timeoutSeconds))
                .collect(Collectors.toList());

        inactiveList.stream().forEach(entry -> {
            if (_aggregateRootInfoDict.remove(entry.getKey()) != null) {
                _logger.info("Removed inactive aggregate root, id: %s", entry.getKey());
            }
        });
    }
}
