package com.qianzhui.enodesamples.quickstart.providers;

import com.qianzhui.enode.eventing.IDomainEvent;
import com.qianzhui.enode.rocketmq.AbstractTopicProvider;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by junbo_xu on 2016/4/15.
 */
public class EventTopicProvider extends AbstractTopicProvider<IDomainEvent> {
    @Override
    public String getTopic(IDomainEvent command) {
        return "NoteSampleTopic";
    }

    @Override
    public Collection<String> getAllTopics() {
        return new ArrayList<String>() {{
            add("NoteSampleTopic");
        }};
    }
}
