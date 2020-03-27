package com.concurrency.tool.atom;

import sun.plugin.cache.OldCacheEntry;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 解决add10k的问题, 使用无锁方案
 * @author  王智
 * @date  2020年3月27日
 */
public class AtomicLongDemo {

    AtomicLong atomicLong = new AtomicLong(0);

    /**
     * 使用无锁方案解决add10k的问题
     */
    public void add10k() {
        int index = 0;
        while(index++ < 100000) {
            // ++i操作
            atomicLong.incrementAndGet();
            /**
             * public final long accumulateAndGet(long x,
             *                                        LongBinaryOperator accumulatorFunction) {
             *         long prev, next;
             *         do {
             *             prev = get();
             *             next = accumulatorFunction.applyAsLong(prev, x);
             *         } while (!compareAndSet(prev, next));
             *         return next;
             *     }
             */
        }
    }

}

class Simulation {

    volatile long count;

    void add10k() {
        int index = 0;
        while(index++ < 100000) {
            addOne();
        }
    }

    /**
     * 实现count + 1的操作
     */
    void addOne() {
        long oldValue;
        long newValue;
        do{
            oldValue = count;
            newValue = count + 1;
        } while(oldValue != cas(oldValue, newValue));
    }

    /**
     * CAS判断是否存在多线程count值被修改, 当前计算的值和内存中的值是否一样
     * @param value
     * @param newValue
     * @return
     */
    long cas(long value, long newValue) {
        // 记录当前count值
        long curValue = count;
        // 判断当前count(内存)的计算时的值是否相同
        if(curValue == value) {
            count = newValue;
        }
        return curValue;
    }
}
