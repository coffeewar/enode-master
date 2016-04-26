package com.qianzhui.enode.infrastructure;

import java.util.function.Consumer;

/**
 * Created by junbo_xu on 2016/3/21.
 */
public interface When<ExpectType extends Throwable, OrigType extends Throwable> {
    WrappedExceptionParser<OrigType> then(Consumer<ExpectType> consumer);
}
