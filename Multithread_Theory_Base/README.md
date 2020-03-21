### 多线程并发:

#### 全景图

- 分工
- 协作
- 互斥

#### 并发编程的源头

- 可见性 : 一个线程对共享变量的修改对另一个线程可见.(多CPU多线程导致的CPU缓存不可见问题 - VisibilityDemo)
- 原子性 : 操作不可分割.(高级语言多条CPU指令分割带来的原子性问题 - AtomicityDemo)
- 有序性 : 程序的顺序执行.(JVM的编译优化 - OrderlyDemo)

volatile - 禁用缓存, 可见 - VolatileDemo

happen-before - 可见, 有序 - HappenBeforeDemo

锁 - 原子性 - atomicity包下   

#### 并发编程注意的问题

- 安全性问题: 数据竞争和竞态条件
- 活跃性问题: 死锁, 活锁和饥饿
- 性能问题: 串行化带来的性能问题

###### 安全性问题

线程安全: 程序能够按照我们期望的执行下去. 也就是说线程安全理论上是不会产生原子性, 可见性和有序性问题的.

什么时候需要注意线程安全? 那就是并发问题产生的原因: 其实就是共享数据且数据会发生变化(多个线程同时读写数据).

数据竞争: 多个线程共享数据, 并且至少有一个线程会写数据.
```

public class Test {
  private long count = 0;
  void add10K() {
    int idx = 0;
    while(idx++ < 10000) {
      count += 1;
    }
  }
}
```

竞态条件: 程序的执行结果会依赖线程的执行顺序(或者说依赖于某个状态的变量).
```

public class Test {
  private long count = 0;
  synchronized long get(){
    return count；
  }
  synchronized void set(long v){
    count = v;
  } 
  void add10K() {
    int idx = 0;
    while(idx++ < 10000) {
      // 多个线程执行这行, 结果依赖于线程的执行顺序, 同时, 结果是1, 有先后顺序, 结果是2
      set(get()+1)      
    }
  }
}
// 再比说说转账的判断, 转账金额要小于卡内余额, 如果两个线程同时执行这行, 条件都满足, 一个线程减去转账金额后, 这个时候卡内余额
// 可能就小于转账金额了, 这个时候的条件就发生改变, 不满足, 但是线程2已经开始进行了转账操作, 会出现超额的问题

class Account {
  private int balance;
  // 转账
  void transfer(
      Account target, int amt){
    if (this.balance > amt) {
      this.balance -= amt;
      target.balance += amt;
    }
  } 
}

// 解决这类问题就是要依赖于前面说的互斥, 就是使用锁
```

###### 活跃性问题

 - 死锁: 占有资源等待, 不可抢占, 循环等待(目前所学解决方案是等待唤醒机制)
 - 活锁: 线程无阻塞, 但是执行不下去. 举例子独木桥相互谦让, 同时退, 看到对方推又同时进.(解决方案等待随机事件)
 - 饥饿: 因线程无法访问所需的资源而无法进行下去(结局方案: 保证资源充足(不现实), 平均分配资源(使用公平锁), 防止线程占据锁时间过长(不现实))
 
 ###### 性能问题
 
 性能问题就是有可能会过度的使用锁造成程序串行化, 这样就会大大地影响性能.
 
 解决方案:
 - 无锁机制: 线程本地存储(Thread Local Storage, TLS), 写入时复制(Copy-on-write), 乐观锁等, Java并发包原子类等
 - 减少锁持有的时间: 使用细粒度锁, Java里面的额ConcurrentHashMap就是使用分段锁技术; 读写锁等.
 
 
 