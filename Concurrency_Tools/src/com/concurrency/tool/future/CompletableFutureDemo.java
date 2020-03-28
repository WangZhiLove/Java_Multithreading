package com.concurrency.tool.future;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 异步化编程, 实现烧水煮茶
 * @author   王智
 * @date  2020年3月28日
 */
public class CompletableFutureDemo {
    public static void main(String[] args) {

        CompletableFuture<Void> completableFuture1 = CompletableFuture.runAsync(()->{
            System.out.println("T1: 洗水壶");
            sleep(1, TimeUnit.SECONDS);

            System.out.println("T1: 烧水");
            sleep(5, TimeUnit.SECONDS);
        });

        CompletableFuture<String> completableFuture2 = CompletableFuture.supplyAsync(()->{
            System.out.println("T2: 洗茶壶");
            sleep(1, TimeUnit.SECONDS);

            System.out.println("T2: 洗茶杯");
            sleep(1, TimeUnit.SECONDS);

            System.out.println("T2: 取茶叶");
            return "龙井";
        });

        CompletableFuture<String> completableFuture3 = completableFuture1.thenCombine(completableFuture2, (__, ft) -> {

            System.out.println("T3: 拿到茶叶" + ft);
            System.out.println("T3: 煮茶");
            return "上茶";
        });
        System.out.println(completableFuture3.join());


    }


    static void sleep(long l, TimeUnit unit) {
        try {
            unit.sleep(l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
