package com.qianzhui.enode.common.socketing.buffermanagement;

import java.util.List;

/**
 * Created by junbo_xu on 2016/3/7.
 */
public interface IPoolItemCreator<T> {

    List<T> create(int count);
}
