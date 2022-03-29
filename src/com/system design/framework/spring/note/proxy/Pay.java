package com.spring.note.proxy;

/**
 * @ClassName Pay
 * @Description TODO
 * @Author 0XuX0
 * @Date 2020/6/20
 **/
public class Pay implements IPay {
    @Override
    public void pay() {
        System.out.println("I have payed for this goods ...");
    }
}
