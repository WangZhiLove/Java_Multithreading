package com.multirhreading.memory.model.happen.before;

/**
 * 并发编程的bug源头就是可见性、原子性和有序性, 而造成这三点的原因是缓存和编译优化,
 * 缓存和编译优化的目的是提高性能, 如果禁用缓存和编译优化, 那程序的性能就会带来很大问题,
 * 要解决这个问题, 需要按需禁用缓存和编译优化, 实现的方式就是
 *      volatile、synchronized和final关键字和六项happens-bofore规则
 *
 * Happens-Before的意思不是先行发生, 而是前面一个操作的结果对后续操作是可见的.
 * @author 王智
 * @date   2020年3月12日
 */
public class HappenBeforeDemo {


    // Happens-Before规则一: 程序的顺序性规则
    //  在一个线程中, 按照顺序执行, 前面的操作必须Happens-Before与后面的操作

    // Happens-Before规则二: volatile变量规则
    //  对于volatile变量的写操作一定Happens-Before于后面的读操作

    // Happens-Before规则三: 传递性
    //  A Happens-Before B, B Happens-Before C, 那么A Happens-Before C



    class VolatileExample {
        int x = 0;
        volatile boolean v = false;
        public void writer() {
            x = 42;    // A
            v = true;  // B
        }
        public void reader() {
            if (v == true) { // C
                // 这里x会是多少呢？  x = 42
                // A Happens-Before B 是规则1
                // B Happens-Before C 是规则2
                // 按照规则3  A Happens-Before C
            }
        }
        public void test() {
            synchronized (this) { //此处自动加锁
                // x是共享变量,初始值=10
                if (this.x < 12) {
                    this.x = 12;
                }
            } //此处自动解锁
        }
    }

    // Happens-Before规则四: 管程中锁的规则
    //  这条规则是指对一个锁的解锁要Happens-Before于后续对这个锁的加锁
    //  管程的概念其实就是一种通用的同步原语, 在Java中就是synchronized关键字, synchronized就是java中对管程的实现
    //  synchronized的锁是隐式的, 进入同步块之前自动加锁, 同步块结束自动解锁

    // Happens-Before规则五: 线程start()规则
    //  主线程A启动子线程B, 子线程B可以看到主线程启动子线程B之前的操作.

    /*Thread B = new Thread(()->{
        // 主线程调用B.start()之前
        // 所有对共享变量的修改，此处皆可见
        // 此例中，var==77
    });
    // 此处对共享变量var修改
        var = 77;
    // 主线程启动子线程
    B.start();*/

    // Happens-Before规则六: 线程join()规则
    //  主线程A等待线程B完成, 线程B完成后, 线程A可以看到线程B中对共享变量的操作

    /*Thread B = new Thread(()->{
        // 此处对共享变量var修改
        var = 66;
    });
    // 例如此处对共享变量修改，
    // 则这个修改结果对线程B可见
    // 主线程启动子线程
    B.start();
    B.join()
    // 子线程所有对共享变量的修改
    // 在主线程调用B.join()之后皆可见
    // 此例中，var==66*/


    // TODO 思考题 有一个共享变量 abc，在一个线程里设置了 abc 的值 abc=3，你思考一下，有哪些办法可以让其他线程能够看到abc==3？
}
