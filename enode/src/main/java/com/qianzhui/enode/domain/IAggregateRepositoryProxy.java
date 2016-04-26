package com.qianzhui.enode.domain;

import com.qianzhui.enode.infrastructure.IObjectProxy;

/**
 * Created by junbo_xu on 2016/4/1.
 */
public interface IAggregateRepositoryProxy extends IObjectProxy {
    IAggregateRoot get(String aggregateRootId);
}
