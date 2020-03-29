package mcjty.lostcities.cubic.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ClassFactory implements InvocationHandler {
    private final Object o;

    public ClassFactory(Object o) {
        this.o = o;
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        Method method = o.getClass().getMethod(m.getName(), m.getParameterTypes());
        return method.invoke(o, args);
    }
}
