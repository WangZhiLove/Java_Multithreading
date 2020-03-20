package com.multirhreading.atomicity;

/**
 * 使用互斥锁解决原子性问题, 都知道原子性问题带来Bug的原因就是线程(CPU)切换, 那如果同一时刻只有一个线程访问,
 * 那就不会存在Bug了, 所以我们只要保证这个就可以禁止线程切换, 解决问题了
 * @author  王智
 * @date  2020年3月18日
 */
public class SolveAtomicity {

    long count = 0;

    long countLock = 0;

    static long countTwoLock = 0;

    /**
     * 互斥, 能想到的就是锁了, 加锁就可以保证互斥了, 但是在使用锁的时候要明确两点:
     * 1. 锁的是什么
     * 2. 保护的是什么
     * 这两点很重要, 只有明确这两点, 才能确保线程不会切换, 才能确保原子性的bug不会发生
     *
     * 资源与锁之间的关系应该是 N:1 的关系, 这点需要格外注意
     */

    /**
     * Java中提供的锁技术: synchronized
     * synchronized会隐式的lock和unlock
     * 隐式规则:
     *  修饰静态方法锁定的的是当前类(Class)
     *  修饰非静态方法锁定的是当前对象(this)
     */

    public void addOne() {
        this.count += 1;
    }

    public long getCount() {
        return count;
    }

    public synchronized void addLockOne() {
        this.countLock += 1;
    }

    public synchronized long getLockCount() {
        return countLock;
    }

    public static void addTwoLockOne() {
        countTwoLock += 1;
    }

    public synchronized long getTwoLockCount() {
        return countTwoLock;
    }

    public static void main(String[] args) throws InterruptedException {
        SolveAtomicity solveAtomicity = new SolveAtomicity();
        // 未加锁
        for (int i = 0; i < 200000; i++) {
            new Thread(() -> {
                solveAtomicity.addOne();
            }).start();
        }
        Thread.sleep(60000);
        // 得到的结果不是200000, 说明了线程切换带来的Bug
        System.out.println("count : " + solveAtomicity.getCount());

        // 加锁
        for (int i = 0; i < 200000; i++) {
            new Thread(() -> {
                solveAtomicity.addLockOne();
            }).start();
        }
        Thread.sleep(60000);
        // 得到的结果是200000, 注意的是get set都要加锁, 否则只保护了写操作, 那读操作也会存在并发问题
        System.out.println("lockCount: " + solveAtomicity.getLockCount());

        // 两把锁保护同一资源的情况, 写方法锁的对象是类SolveAtomicity, 读方法锁的对象是当前对象this, 虽然保护的资源都是countTwoLock, 但是并不能保证并发性问题
        for (int i = 0; i < 200000; i++) {
            new Thread(() -> {
                addTwoLockOne();
            }).start();
        }
        Thread.sleep(60000);
        // 得到的结果不是200000, 注意的是可以用一个锁保护多个资源, 但是不能用多把锁保护一个资源
        System.out.println("twoLockCount: " + solveAtomicity.getTwoLockCount());
    }

    /**
     * 下面的代码可以解决原子性和可见性问题吗?
     */

    class SafeCalc {
        long value = 0L;
        long get() {
            synchronized (new Object()) {
                return value;
            }
        }
        void addOne() {
            synchronized (new Object()) {
                value += 1;
            }
        }
    }

    // 并不能, 原因是锁的对象永远是个新对象, 永远也不可能锁得住
}
