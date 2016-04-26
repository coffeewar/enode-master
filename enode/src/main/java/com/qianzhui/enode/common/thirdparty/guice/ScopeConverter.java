package com.qianzhui.enode.common.thirdparty.guice;

import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.qianzhui.enode.common.container.LifeStyle;

/**
 * Created by junbo_xu on 2016/2/27.
 */
public class ScopeConverter {
    public static Scope toGuiceScope(LifeStyle life) {
        if (life == null)
            return Scopes.SINGLETON;
        switch (life) {
            case Transient:
                return Scopes.NO_SCOPE;
            default:
                return Scopes.SINGLETON;
        }
    }
}
