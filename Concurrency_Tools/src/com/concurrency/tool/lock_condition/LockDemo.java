package com.concurrency.tool.lock_condition;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Lock的使用, 以及Lock是如何保证可见性的理解
 * @author 王智
 * @date  2020年3月22日
 */
public class LockDemo {

    /**
     * 可重入锁
     */
    private final Lock lock = new ReentrantLock();

    int value;

    /**
     * 给 value + 1
     * 如何保证多个线程之间value值共享且不会出现并发问题呢? 利用Happen-Before, 首先要知道Lock里面有一个volatile变量
     * state(AbstractQueuedSynchronizer.java中), 每次进行加锁和释放锁的时候都会进state进行读写.
     * 1. 顺序性规则: value的写操作Happen-Before对state的读写操作, 也就是vale += 1 Happen-Before lock.unlock
     * 2. volatile规则: volatile变量的写操作Happen-Before他的读操作, 所以线程A的lock.unlock Happen-Before B线程
     * 的lock.lock
     * 3. 传递规则: value += 1 就Happen-before与线程B的lock.lock操作
     *
     * 这就保证了多个线程对共享变量的可见性问题
     */
    public void addValue() {
        lock.lock();
        try {
            value += 1;
        } finally {
            lock.unlock();
        }
    }


}


