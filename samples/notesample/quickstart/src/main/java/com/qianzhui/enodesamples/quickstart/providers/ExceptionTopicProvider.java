package com.qianzhui.enodesamples.quickstart.providers;

import com.qianzhui.enode.infrastructure.IPublishableException;
import com.qianzhui.enode.rocketmq.AbstractTopicProvider;
import com.qianzhui.enode.rocketmq.TopicTagData;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by junbo_xu on 2016/6/5.
 */
public class ExceptionTopicProvider extends AbstractTopicProvider<IPublishableException> {
    @Override
    public TopicTagData getPublishTopic(IPublishableException event) {
        return new TopicTagData("NoteSampleTopic", "Exception");
    }

    @Override
    public Collection<TopicTagData> getAllSubscribeTopics() {
        return new ArrayList<TopicTagData>() {{
            add(new TopicTagData("NoteSampleTopic", "Exception"));
        }};
    }
}
