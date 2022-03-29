package com.spring.note.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @ClassName DynamicProxyHandler
 * @Description TODO
 * @Author 0XuX0
 * @Date 2020/6/20
 **/
public class DynamicProxyHandler implements InvocationHandler {

    private Object object;

    public DynamicProxyHandler(Object object) {
        this.object = object;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("Before Dynamic proxy handle ...");
        Object res = method.invoke(object, args);
        System.out.println("After Dynamic proxy handle ...");
        return res;
    }
}
