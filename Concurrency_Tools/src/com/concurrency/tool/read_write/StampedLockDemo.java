package com.concurrency.tool.read_write;

import java.util.concurrent.locks.StampedLock;

/**
 * 读写锁的另一种实现  - 乐观读
 * @author  王智
 * @date    2020年3月24日
 */
public class StampedLockDemo {

    final StampedLock stampedLock = new StampedLock();

    /**
     * 悲观读
     */
    void read() {
        long l = stampedLock.readLock();
        try {
            // 处理业务逻辑
        } finally {
            stampedLock.unlockRead(l);
        }
    }

    /**
     * 写
     */
    void write() {
        long l = stampedLock.writeLock();
        try {
            // 业务逻辑
        } finally {
            stampedLock.unlockWrite(l);
        }

    }

}

class Point {
    private int x, y;

    final StampedLock sl = new StampedLock();

    /**
     * 乐观读操作者期间如果存在写操作, 就将乐观读升级为悲观读锁, 以达到阻塞写线程
     * @return
     */
    int distanceFromOrigin() {
        // 乐观读
        long stamp = sl.tryOptimisticRead();
        // 读入局部变量, 这个时候局部变量可能被修改
        int curX = x, curY = y;
        // 判断执行读操作之间有没有写操作
        if(!sl.validate(stamp)) {
            // 升级悲观锁
            stamp = sl.readLock();
            try{
                curX = x;
                curY = y;
            } finally {
                // 释放悲观读锁
                sl.unlockRead(stamp);
            }
        }
        return (int)Math.sqrt(curX * curX + curY * curY);

    }
}
