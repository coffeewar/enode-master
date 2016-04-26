package com.qianzhui.enode.domain.impl;

import com.qianzhui.enode.domain.IAggregateRoot;
import com.qianzhui.enode.domain.IAggregateRootFactory;

/**
 * Created by junbo_xu on 2016/4/1.
 */
public class DefaultAggregateRootFactory implements IAggregateRootFactory {

    @Override
    public <T extends IAggregateRoot> T createAggregateRoot(Class<T> aggregateRootType) {
        try {
            return aggregateRootType.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*public IAggregateRoot createAggregateRoot(Class aggregateRootType)
    {
        return FormatterServices.GetUninitializedObject(aggregateRootType) as IAggregateRoot;
    }*/
}
