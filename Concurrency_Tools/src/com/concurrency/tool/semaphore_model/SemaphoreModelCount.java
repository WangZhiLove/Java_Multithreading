package com.concurrency.tool.semaphore_model;

import java.util.concurrent.Semaphore;

/**
 * 信号量模型实现计数器
 * @author  王智
 * @date  2020年3月23日
 */
public class SemaphoreModelCount {
    /**
     * 计数器
     */
    static int count;

    /**
     * 信号量模型, 设置信号量计数器的值是1, 也就是同一时刻只允许一个线程访问
     * 那如果设置的数值大于1呢? 那就是同一时刻允许多个线程并行, 什么时候使用呢?
     * 线程池或者对象池的时候使用
     */
    static final Semaphore semaphore = new Semaphore(1);

    /**
     * 执行计数器加一的操作
     */
    public void addOne() throws InterruptedException {
        semaphore.acquire();
        try {
            count += 1;
        } finally {
            semaphore.release();
        }
    }

}
