## 并发工具类的使用

#### Lock和Condition的使用

Java SDK的核心就是对管程的实现, 而对管程的实现就依赖于Lock和Condition, Lock负责互斥, 
Condition负责同步.

Java中的synchronized就是对管程的实现, 为什么还需要Lock和Condition呢, 因为synchronized获取不到
锁的时候会阻塞, 这个时候的线程啥也干不了, 发生死锁的时候没有办法处理, 但是Lock可以处理死锁的情况.

##### Lock对死锁的处理

前面对于死锁产生的原因有三种做了分析, 处理方法就是根据原因来得到的, 而Lock对于死锁的处理是利用了
不可抢占资源这点, 给出了三种方案:
- 能够响应中断: synchronized获取不到锁的时候会阻塞, 如果死锁就再也没办法唤醒,也就释放不了已经获取到的锁,
如果我们可以有方法让阻塞的线程中断(阻塞线程响应中断), 那就有可能让该线程释放已经拿到的锁, 进而避免死锁. 
- 支持超时: 在一定时间内获取不到锁, 就自行中断或返回错误, 而不进入阻塞, 这也有机会释放已经拿到的锁
- 非阻塞的获取锁: 获取锁失败后, 直接返回, 这也有可能释放曾经持有的锁

上面的三种方案都是破坏了不可抢占这一条件来解决死锁问题的, 这三种方案在Lock中的体现就是:
- 支持中断的API: void lockInterruptibly() throws InterruptedException;
- 支持超时的API: boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
- 支持非阻塞获取锁的API: boolean tryLock();

##### Lock如何保证可见性的

详细的讲解查看LockDemo.java

##### 什么是可重入锁

创建Lock的使用的是ReentrantLock, ReentrantLock就是可重入锁, 意思就是线程可以重复获取同一把锁, 当然这里的
线程指的是同一个线程, 如下:
```

class X {
  private final Lock rtl =
  new ReentrantLock();
  int value;
  public int get() {
    // 获取锁
    rtl.lock();         // 2
    try {
      return value;
    } finally {
      // 保证锁能释放
      rtl.unlock();
    }
  }
  public void addOne() {
    // 获取锁
    rtl.lock();  
    try {
      value = 1 + get(); // 1
    } finally {
      // 保证锁能释放
      rtl.unlock();
    }
  }
}
```

执行到1的时候会调用get方法, 到2之后会再次加锁, 如果rtl是不可重入的, 执行到2的时候就会阻塞.

可重入函数是什么呢? 可重入函数就指的是多个线程同时调用该函数都会得到一个正确的结果, 也就是线程安全.

##### 公平锁和非公平锁

```

//无参构造函数：默认非公平锁
public ReentrantLock() {
    sync = new NonfairSync();
}
//根据公平策略参数创建锁
public ReentrantLock(boolean fair){
    sync = fair ? new FairSync() 
                : new NonfairSync();
}
```

什么时候有用呢?    受到阻塞的线程会进入到等待队列中, 当条件满足需要唤醒线程的时候, 公平锁会唤醒等待时间最长的线程, 
非公平锁是在当前有新的线程请求的时候就不会到等待队列中唤醒, 而是直接执行新的线程, 这样的话等待队列中的线程可能永远不会唤醒 

##### 用锁的最佳实践

- 永远只在更新对象成员变量的时候加锁
- 永远只在获取对象成员变量的时候加锁
- 永远不再调用其他方法的时候加锁(双重加锁可能导致死锁)

```

class Account {
  private int balance;
  private final Lock lock
          = new ReentrantLock();
  // 转账
  void transfer(Account tar, int amt){
    while (true) {
      if(this.lock.tryLock()) {   //  尝试获取锁(非阻塞式获取锁)
        try {
          if (tar.lock.tryLock()) {  //  尝试获取锁, 获取不到就让出资源, 直接返回(非阻塞式获取锁)
            try {
              this.balance -= amt;
              tar.balance += amt;
            } finally {
              tar.lock.unlock();
            }
          }//if
        } finally {
          this.lock.unlock();
        }
      }//if
    }//while
  }//transfer
}
```

这段代码会造成死锁吗? 答案是并不会, 但是会造成活锁, 线程相互谦让导致谁也获取不到锁