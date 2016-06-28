package com.qianzhui.enode.rocketmq;

import java.util.Collection;

/**
 * Created by junbo_xu on 2016/3/2.
 */
public interface ITopicProvider<T> {
    TopicTagData getPublishTopic(T source);

    Collection<TopicTagData> getAllSubscribeTopics();
}
