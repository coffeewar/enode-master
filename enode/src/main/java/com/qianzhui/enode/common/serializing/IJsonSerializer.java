package com.qianzhui.enode.common.serializing;

/**
 * Created by junbo_xu on 2016/3/2.
 */
public interface IJsonSerializer {
    String serialize(Object obj);

    <T> T deserialize(String aSerialization, final Class<T> aType);
}
