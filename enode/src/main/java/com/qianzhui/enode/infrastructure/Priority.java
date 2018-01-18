package com.qianzhui.enode.infrastructure;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by junbo_xu on 2016/3/27.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Priority {

    int value() default 0;
}
