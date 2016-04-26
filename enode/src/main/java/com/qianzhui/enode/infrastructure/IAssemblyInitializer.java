package com.qianzhui.enode.infrastructure;

import java.util.Set;

/**
 * Created by junbo_xu on 2016/3/25.
 */
public interface IAssemblyInitializer {
    void initialize(Set<Class<?>> componentTypes);
}
