package com.concurrency.tool.semaphore_model;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

/**
 * 用信号量模型实现限流器
 * @author  王智
 * @date   2020年3月23日
 */

public class CurrentLimiter {

    public static void main(String[] args) throws InterruptedException {
        // 创建对象池
        ObjPool pool = new ObjPool(10, 2);
        // 通过对象池获取t，之后执行
        pool.exec(t -> {
            System.out.println(t);
            return t.toString();
        });
    }

}

class ObjPool<T, R> {
    /**
     * 对象池
     */
    final List<T> pool;

    /**
     * 信号量模型
     */
    final Semaphore semaphore;

    /**
     * 构造方法初始化对象池和信号量模型的允许同时进入临界区的线程数
     * @param size  线程数
     * @param t     对象
     */
    public ObjPool(int size,T t) {
        // 为什么使用Vector这种线程安全的集合, 为的是exec里面的remove和add操作
        pool = new Vector<>();
        for (int i = 0; i < size; i++) {
            pool.add(t);
        }
        semaphore = new Semaphore(size);
    }

     R exec(Function<T, R> func) throws InterruptedException {
        T t = null;
        semaphore.acquire();
        try {
            t = pool.remove(0);
            return func.apply(t);
        } finally {
            pool.add(t);
            semaphore.release();

        }
    }
}