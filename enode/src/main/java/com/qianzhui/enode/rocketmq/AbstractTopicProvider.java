package com.qianzhui.enode.rocketmq;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by junbo_xu on 2016/3/29.
 */
public abstract class AbstractTopicProvider<T> implements ITopicProvider<T> {
    private Map<Class, String> _topicDict = new HashMap<>();

    @Override
    public String getTopic(T source) {
        return _topicDict.get(source.getClass());
    }


    @Override
    public Collection<String> getAllTopics() {
        return _topicDict.values();
    }

    protected Collection<Class> getAllTypes() {
        return _topicDict.keySet();
    }

    protected void registerTopic(String topic, Class[] types) {
        if (types == null || types.length == 0)
            return;

        for (int i = 0, len = types.length; i < len; i++) {
            _topicDict.put(types[i], topic);
        }
    }
}
