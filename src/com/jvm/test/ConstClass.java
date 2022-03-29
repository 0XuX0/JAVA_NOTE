package com.jvm.test;

/**
 * @ClassName ConstClass
 * @Description TODO
 * @Author 0XuX0
 * @Date 2020/5/12
 **/
public class ConstClass {
    static {
        System.out.println("ConstClass Init !");
    }
    public static final String HEELOWORLD = "hello world";
}
