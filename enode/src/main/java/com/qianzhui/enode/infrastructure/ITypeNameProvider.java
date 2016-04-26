package com.qianzhui.enode.infrastructure;

/**
 * Created by junbo_xu on 2016/3/8.
 */
public interface ITypeNameProvider {
    String getTypeName(Class type);

    Class getType(String typeName);
}
