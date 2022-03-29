package com.spring.note.proxy;

/**
 * @ClassName PayProxy
 * @Description TODO
 * @Author 0XuX0
 * @Date 2020/6/20
 **/
public class PayProxy implements IPay{

    private Pay pay;

    public PayProxy(Pay pay) {
        this.pay = pay;
    }

    @Override
    public void pay() {
        // Before pay ...
        System.out.println("Plz make sure u have enough money ...");

        pay.pay();

        // After pay ...
        System.out.println("Welcome next time");
    }
}
