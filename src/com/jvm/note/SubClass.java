package com.jvm.note;

import com.jvm.test.SuperClass;

/**
 * @ClassName SubClass
 * @Description TODO
 * @Author 0XuX0
 * @Date 2020/5/12
 **/
public class SubClass extends SuperClass {
    static {
        System.out.println("SubClass Init !");
    }
}
