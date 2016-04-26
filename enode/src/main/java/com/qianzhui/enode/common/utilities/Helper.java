package com.qianzhui.enode.common.utilities;

import com.qianzhui.enode.common.function.Action;
import com.qianzhui.enode.common.function.Func;

/**
 * Created by junbo_xu on 2016/3/5.
 */
public class Helper {
    public static void eatException(Action action) {
        try {
            action.apply();
        } catch (Exception e) {
        }
    }

    public static <T> T eatException(Func<T> action, T defaultValue) {
        try {
            return action.apply();
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
