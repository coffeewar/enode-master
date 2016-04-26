package com.qianzhui.enode.eventing;

import java.util.List;
import java.util.Map;

/**
 * Created by junbo_xu on 2016/3/20.
 */
public interface IEventSerializer {
    Map<String, String> serialize(List<IDomainEvent> evnts);

    <TEvent extends IDomainEvent> List<TEvent> deserialize(Map<String, String> data, Class<TEvent> domainEventType);
}
