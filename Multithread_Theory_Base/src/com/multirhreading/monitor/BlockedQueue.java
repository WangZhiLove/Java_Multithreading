package com.multirhreading.monitor;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 管程实现同步的原理 - 等待唤醒机制
 * @param <T>
 * @author 王智
 * @date  2020年3月21日
 */
public class BlockedQueue<T> {

    Queue queue = new ConcurrentLinkedQueue();

    final Lock lock = new ReentrantLock();

    /**
     * 条件变量, 队列不满
     */
    final Condition notFull = lock.newCondition();

    /**
     * 条件变量, 队列不空
     */
    final Condition notEmpty = lock.newCondition();

    /**
     * 对于入队操作，如果队列已满，就需要等待直到队列不满，所以这里用了notFull.await();。
     * 对于出队操作，如果队列为空，就需要等待直到队列不空，所以就用了notEmpty.await();。
     * 如果入队成功，那么队列就不空了，就需要通知条件变量：队列不空notEmpty对应的等待队列。
     * 如果出队成功，那就队列就不满了，就需要通知条件变量：队列不满notFull对应的等待队列。
     *
     * await() 和前面我们提到的 wait() 语义是一样的；signal() 和前面我们提到的 notify() 语义是一样的。
     *
     * 与我们synchronized不同的是管程是有多个条件变量的, 而synchronized只支持一个
     */

    /**
     * 入队操作
     * @param x
     */
    void equ(T x) {
        lock.lock();
        try {
            // "队列已满"的判断
            while(queue.size() == 10) {
                notFull.await();
            }
            // 入队操作
            // 入队操作完成后
            notEmpty.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }

    /**
     * 出队操作
     * @param x
     */
    void deq(T x) {
        lock.lock();
        try {
            // "队列已满"的判断
            while(queue.isEmpty()) {
                notEmpty.await();
            }
            // 出队操作
            // 出队操作完成后
            notFull.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }

}
