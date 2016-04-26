package com.qianzhui.enode.common.container;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by junbo_xu on 2016/3/8.
 */
public class GenericTypeLiteral<T> {

    private Type type;

    protected GenericTypeLiteral(Type type){
        this.type = type;
    }

    protected GenericTypeLiteral() {
        Type superClass = getClass().getGenericSuperclass();

        if (superClass instanceof Class) {
            throw new RuntimeException("Missing type parameter.");
        }

        ParameterizedType parameterized = (ParameterizedType) superClass;
        type = parameterized.getActualTypeArguments()[0];
    }

    public static GenericTypeLiteral<?> get(Type type) {
        return new GenericTypeLiteral<Object>(type);
    }

    public Type getType() {
        return type;
    }

    public String key(){
        return type.getTypeName();
    }
}
