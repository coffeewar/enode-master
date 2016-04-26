package com.qianzhui.enode.rocketmq;

import java.util.Collection;

/**
 * Created by junbo_xu on 2016/3/2.
 */
public interface ITopicProvider<T> {
    String getTopic(T source);

    Collection<String> getAllTopics();
}
