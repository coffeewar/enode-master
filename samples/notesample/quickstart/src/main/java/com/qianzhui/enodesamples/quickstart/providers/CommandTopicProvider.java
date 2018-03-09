package com.qianzhui.enodesamples.quickstart.providers;

import com.qianzhui.enode.commanding.ICommand;
import com.qianzhui.enode.rocketmq.AbstractTopicProvider;
import com.qianzhui.enode.rocketmq.TopicTagData;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by junbo_xu on 2016/4/15.
 */
public class CommandTopicProvider extends AbstractTopicProvider<ICommand> {
    @Override
    public TopicTagData getPublishTopic(ICommand command) {
        return new TopicTagData("EnodeCommonTopicDev", "Command");
    }

    @Override
    public Collection<TopicTagData> getAllSubscribeTopics() {
        return new ArrayList<TopicTagData>() {{
            add(new TopicTagData("EnodeCommonTopicDev", "Command"));
        }};
    }
}
