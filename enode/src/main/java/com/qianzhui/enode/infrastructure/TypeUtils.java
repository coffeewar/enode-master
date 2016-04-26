package com.qianzhui.enode.infrastructure;

import com.qianzhui.enode.domain.IAggregateRoot;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by junbo_xu on 2016/3/29.
 */
public class TypeUtils {
    public static boolean isComponent(Class type) {
        return !Modifier.isAbstract(type.getModifiers()) && type.isAnnotationPresent(Component.class);
    }

    public static boolean isAggregateRoot(Class type) {
        return !Modifier.isAbstract(type.getModifiers()) && IAggregateRoot.class.isAssignableFrom(type);
    }

    public static Type getSuperGenericInterface(Class implementerType, Class toResolve) {
        Type[] genericInterfaces = implementerType.getGenericInterfaces();

        for (int i = 0, len = genericInterfaces.length; i < len; i++) {
            Type genericInterface = genericInterfaces[i];

            if (genericInterface == toResolve)
                return genericInterface;

            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType genericInterfaceType = (ParameterizedType) genericInterface;

                if (genericInterfaceType.getRawType() == toResolve)
                    return genericInterfaceType;
            }
        }

        //查找一级父类（只支持一层）
        Type genericSuperclass = implementerType.getGenericSuperclass();
        if (genericSuperclass == null)
            return null;

        if (genericSuperclass instanceof Class) {
            if (toResolve.isAssignableFrom((Class) genericSuperclass)) {
                return toResolve;
            }
            return null;
        }

        ParameterizedType genericSuperclassType = (ParameterizedType) genericSuperclass;

        Type superGenericRawType = genericSuperclassType.getRawType();

        if (!(superGenericRawType instanceof Class))
            return null;

        Class<?>[] interfaces = ((Class) superGenericRawType).getInterfaces();
        for (int i = 0, len = interfaces.length; i < len; i++) {
            Class<?> anInterface = interfaces[i];
            if (anInterface == toResolve) {
                //假设父类与目标接口泛型一致
                return ParameterizedTypeImpl.make(toResolve, genericSuperclassType.getActualTypeArguments(), null);
            }
        }

        return null;
    }
}
