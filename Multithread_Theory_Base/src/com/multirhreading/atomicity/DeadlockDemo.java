package com.multirhreading.atomicity;

import java.util.ArrayList;
import java.util.List;

/**
 * 死锁的demo
 * 使用细粒度锁造成死锁的问题, 就是锁定资源的时候使用多个锁
 * @author  王智
 * @date  2020年3月20日
 */
public class DeadlockDemo {
}


/**
 * 死锁, 下面使用的细粒度锁就会出现死锁的问题. 前面说用DeadlockAccount.class来作为锁, 为什么不可以呢?
 *  因为使用对象锁, 会造成整个项目的执行串行化, 效率会大大降低, 所以使用细粒度锁可以提高效率
 *
 *  为什么会产生死锁? 举个例子 A -> B(A线程), B -> A(B线程), 在A转账给B的时候, 锁定了A账户, B转账给A的时候锁定了B账户,
 *  这个时候A线程要获取B的锁, B线程持有B的锁, 但要等待A线程释放A的锁, 这种情况下A线程和B线程就会永久等待下去, 这就是死锁
 *
 *  那下面的死锁怎么处理呢? 首先要解决死锁, 就必须要知道死锁产生的原因:
 *  1. 互斥, 访问同一资源
 *  2. 占有且等待, A线程持有A的锁, 等待B的时候不是释放A的锁
 *  3. 不可抢占, 其他线程不可抢占A线程占有的资源
 *  4. 循环等待, A线程等待B线程占有资源的锁, B线程等待A线程占有资源的锁, 这就是循环等待
 */
class DeadlockAccount {

    private Integer account;

    public void transfer(Integer num, DeadlockAccount target) {
        // 锁定转出账户
        synchronized (this) {
            // 锁定转入账户
            synchronized (target) {
                if(this.account > num) {
                    this.account -= num;
                    target.account += num;
                }
            }
        }
    }

}

/**
 * 解决死锁就是破坏死锁产生的条件, 互斥是避免不了的, 能避免的只有2,3和4
 * 破坏占用且等待, 如何做? 其实只要一次获取两个资源的锁, 就可以避免掉死锁.
 * 如何做呢? 其实只需要一个管理者, 在线程获取锁的时候想管理者一次申请全部的资源锁, 如果线程想要的全部的资源不全都有, 那就一个
 * 资源也不给, 这就可以避免死锁了 - 破坏占用且等待(要注意的是管理者只能有一个, 不能有多个)
 */

class Allocator {
    private List<Object> als =
            new ArrayList<>();
    // 一次性申请所有资源
    synchronized boolean apply(
            Object from, Object to){
        if(als.contains(from) ||
                als.contains(to)){
            return false;
        } else {
            als.add(from);
            als.add(to);
        }
        return true;
    }

    /**
     * todo 上面的apply的使用需要借助while死循环的轮询才能完成, 在Java提供了更简单的方法, 就是等待-通知的机制
     * 什么时候等待?
     * 当获取不到想要的全部资源锁的时候等待
     * 什么时候通知呢?
     * 释放全部的资源锁的时候通知
     * 通知是用notify()好还是使用notifyAll()好?
     * 最好使用notifyAll, 因为notify是随机唤醒一个, 在某些情况下有些线程可能永远也不会被唤醒
     * wait()和sleep()的区别是什么? 为什么用wait而不用sleep?
     * wait是等待并释放已经获得的锁
     * sleep是睡眠, 不释放获得的锁
     * 所以应该使用wait才可以避免占有且等待和不可抢占
     */
    synchronized boolean awaitApply(
            Object from, Object to){
        while(als.contains(from) ||
                als.contains(to)){
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            als.add(from);
            als.add(to);
        }
        return true;
    }
    // 归还资源
    synchronized void free(
            Object from, Object to){
        als.remove(from);
        als.remove(to);
        notifyAll();
    }
}

class AllAccount {
    // actr应该为单例(这点很重要)
    private Allocator actr;
    private int balance;
    // 转账
    void transfer(AllAccount target, int amt){
        // 一次性申请转出账户和转入账户，直到成功
        while(!actr.apply(this, target)) ;
        try{
            // 锁定转出账户
            synchronized(this){
                // 锁定转入账户
                synchronized(target){
                    if (this.balance > amt){
                        this.balance -= amt;
                        target.balance += amt;
                    }
                }
            }
        } finally {
            actr.free(this, target);
        }
    }
}

/**
 * 破坏不可抢占就很简单了, 只要在获取资源锁的时候获取不到, 就释放已经获取的锁, 这就破坏了不可抢占, 避免死锁
 */

/**
 * 破坏循环等待呢?破坏这个条件，需要对资源进行排序，然后按序申请资源。这个实现非常简单，我们假设每个账户
 * 都有不同的属性 id，这个 id 可以作为排序字段，申请的时候，我们可以按照从小到大的顺序来申请。
 * 比如下面代码中，①~⑥处的代码对转出账户（this）和转入账户（target）排序，然后按照序号从小到大的顺序锁定账户。
 * 这样就不存在“循环”等待了。
 */


class AwaitAccount {
    private int id;
    private int balance;
    // 转账
    void transfer(AwaitAccount target, int amt){
        AwaitAccount left = this;       // 1
        AwaitAccount right = target;    // 2
        if (this.id > target.id) {      // 3
            left = target;              // 4
            right = this;               // 5
        }                               // 6
        // 锁定序号小的账户
        synchronized(left){
            // 锁定序号大的账户
            synchronized(right){
                if (this.balance > amt){
                    this.balance -= amt;
                    target.balance += amt;
                }
            }
        }
    }
}
