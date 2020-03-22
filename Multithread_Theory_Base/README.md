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
 
 
 #### 管程 - 并发编程的万能钥匙
 
 管程指的是管理共享变成并操作共享变量的过程, 让他们支持并发(这也就意味着管程其实是一系列的动作).
 
 管程的三种模型:
 - Hasen模型
 - Hoare模型
 - MESA模型(Java参考)
 
 ##### MESA模型
 
 并发编程领域的两大问题就是互斥(同一时刻只允许一个线程访问共享变量)和同步(线程之间的通信, 协作).
 
 管程如何解决互斥问题? 这个可以想象一个队列数据结构, 共享变量是一个队列, 操作是入队和出队, 互斥其实就是在入队和出队的操作上加锁
 保证了入队和出队这两个操作互斥, 多个线程要想访问队列, 只能使用提供的入队和出队方法, 这就保持了互斥.
```
monitor X {
    // 共享变量, 队列
    var queue;
    // 入队操作
    func equ();
    // 出队操作
    func deq();
}
 感觉这种其实就是synchronized的互斥实现
```

管程如何解决同步问题呢? 管程解决同步问题其实就是前面说的等待唤醒机制. -  BlockedQueue类

###### wait()

while循环中使用wait, 这是MESA模型特有的.
```
while(条件不满足) {
  wait();
}
```

不同模型的核心区别之一就是当条件满足后, 如何通知相关线程:
- Hasen模型中, notify要求放在代码的最后, 也就是说线程T1执行到最后, 通知T2线程, 这个时候T1也就执行完成了.T2开始执行, 保证同一时刻
只有一个线程执行
- Hoare模型中, 条件满足之后, T1线程立即通知T2线程, T2线程开始运行, T1线程阻塞, T2执行完成之后, T1才继续执行, 多了一步阻塞操作.
- MESA模型中, T1通知T2后, T1会继续执行, T2不会立即执行, 而是从条件变量队列中进入到入口等待队列中(阻塞状态变成可运行状态, 等待系统资源调度), 
好处是notify不用放在代码最后, 也没有多于的阻塞操作, 坏处就是当T2开始执行的时候可能前面满足的条件现在又不满足了, 这就需要用循环方式检验队列了.

###### notify() 和 notifyAll()

一般情况下, 全部使用notifyAll(), 不要轻易使用notify(), 使用notify的三个必要条件:

- 所有的等待线程拥有相同的条件
- 唤醒之后执行的操作都相同
- 只需要唤醒一个线程


#### 线程的生命周期

通用线程的生命周期:
- 初始状态(线程被创建, 不允许分配CPU执行, 编程语言方面创建, 操作系统方面并没有创建)
- 可运行状态(可以分配CPU执行)
- 运行状态(可运行状态线程获取CPU, 进入到运行状态)
- 休眠状态(运行状态线程调用阻塞式API, 进入休眠状态, 释放CPU使用权)
- 终止状态(线程执行完或者异常退出)

重点在于运行状态, 休眠状态, 可运行状态之间的转变切换, 其实就是运行状态 -> 休眠状态 -> 可运行状态 -> 运行状态

#### Java线程的生命周期

说Java的线程生命周期首先要保证自己理解 **JVM不关心操作系统底层线程调度的状态**
- NEW(初始化状态)
- RUNNABLE(可运行状态/运行状态)
- BLOCKED(阻塞状态)
- WAITING(无时限等待)
- TIMED_WAITING(有时限等待)
- TERMINATED(阻塞状态) 

其实BLOCKED, WAITING和TIMED_WAITING是一种状态, 就是休眠, 只要Java线程处在这一个状态, 那么就永远得不到CPU的使用权.

###### RUNNABLE和BLOCKED状态之间的转化

只有一种情况下会发生这种转换, 就是等待synchronized隐式锁, synchronized修饰的只允许一个线程访问, 其他线程等待synchronized锁的
时候线程状态就会转变为BLOCKED状态, 获得锁之后就会转换为RUNNABLE, 注意的是这只是Java层面线程的状态变化, 不是操作系统的线程
状态变化, 也就是说当Java处在BLOCKED状态的时候, 操作系统中的该线程的状态还是RUNNABLE.

###### RUNNABLE和WAITING状态之间的转换

三种场景会触发这种转换:
- 调用wait()方法, RUNNABLE -> WAITING, 等待notify()或者notifyAll()之后会 WAITING -> RUNNABLE
- 调用A.join()方法, 当在某个线程中执行A.join(), 那这个线程就会转换为WAITING, 等待A线程执行完成之后, 会再次转换为RUNNABLE
- 调用LockSupport.park()方法, Java并发包中的锁都是基于LockSupport实现的, 调用park方法之后, 当前线程会阻塞, 
调用LockSupport.unpark(Thread thread)可唤醒目标线程, 重新进入RUNNABLE状态.

###### RUNNABLE和TIMED_WAITING之间的装换

有五种场景会触发这种转换：
- 调用带超时参数的 Thread.sleep(long millis) 方法；
- 获得 synchronized 隐式锁的线程，调用带超时参数的 Object.wait(long timeout) 方法；
- 调用带超时参数的 Thread.join(long millis) 方法；
- 调用带超时参数的 LockSupport.parkNanos(Object blocker, long deadline) 方法；
- 调用带超时参数的 LockSupport.parkUntil(long deadline) 方法。

时间到了会自动进入到RUNNABLE状态

###### NEW到RUNNABLE

NEW状态其实就是实现线程或者创建线程过程:
1. 继承Thread类, 重写run方法
```

// 自定义线程对象
class MyThread extends Thread {
  public void run() {
    // 线程需要执行的代码
    ......
  }
}
// 创建线程对象
MyThread myThread = new MyThread();
```
2. 实现Runnable接口, 重写run方法
```

// 实现Runnable接口
class Runner implements Runnable {
  @Override
  public void run() {
    // 线程需要执行的代码
    ......
  }
}
// 创建线程对象
Thread thread = new Thread(new Runner());
```

至于怎么转换到RUNNABLE那就很简单了, 使用线程对象.start()方法就可以了, RUNNABLE不能到NEW状态

###### RUNNABLE到TERMINATED

三种情况:
- 线程执行完毕
- 线程调用了stop方法(已过时)
- 线程调用了interrupt()方法

stop和interrupt方法的区别:
- stop方法会直接杀死线程, 并释放该线程获取的所有的锁, 那以前该线程锁保护的对象就会被损坏, 并且损坏对象对外可见, 不安全.
- interrupt方法, interrupt方法仅仅通知线程, 线程还是有机会执行后续操作, 同时可以无视这个通知, 被interrupt的线程如何
接收通知呢? 一种是异常, 另一种是主动检测.

线程处在等待状态(BLOCKED, WAITING, TIMED_WAITING), 其他线程调用了该线程的interrupt方法, 该线程会转换到RUNNABLE状态,
并抛出InterruptedException异常, 转换BLOCKED, WAITING, TIMED_WAITING的条件是调用wait(), join(), sleep()方法的签名
都抛出了InterruptedException异常, 这个异常的触发条件就是其他线程调用了该线程的interrupt方法

当线程 A 处于 RUNNABLE 状态时，并且阻塞在 java.nio.channels.InterruptibleChannel 上时，
如果其他线程调用线程 A 的 interrupt() 方法，线程 A 会触发 java.nio.channels.ClosedByInterruptException 
这个异常；而阻塞在 java.nio.channels.Selector 上时，如果其他线程调用线程 A 的 interrupt() 方法，
线程 A 的 java.nio.channels.Selector 会立即返回。

上面两种是异常接受通知, 还有一种情况就是主动检测, 自己调用isInterrupted() 方法可以检测自己是否被中断.

jstack 命令或者Java VisualVM这个可视化工具将 JVM 所有的线程栈信息导出来.

```
// 当前这个线程被中断之后可以退出while(true)循环吗?
// 答案是并不能, 因为抛出异常后, 中断标识会被清除, 所以需要重置中断表示, 放开注释的代码就可以
Thread th = Thread.currentThread();
while(true) {
  if(th.isInterrupted()) {
    break;
  }
  // 省略业务代码无数
  try {
    Thread.sleep(100);
  }catch (InterruptedException e){
    //Thread.currentThread().interrupt();
    e.printStackTrace();
  }
}
```

#### 创建多少个线程合适

使用多线程的目的是 降低延迟, 提高吞吐量.

降低延迟是属于算法的范畴, 所以使用多线程就是提高吞吐量, 在并发编程领域，提升性能本质上就是提升硬件的利用率，
再具体点来说，就是提升 I/O 的利用率和 CPU 的利用率。并且我们需要解决 CPU 和 I/O 设备综合利用率的问题。
就是说CPU和I/O操作同时运行.

创建多少个线程合适?
- 对于 CPU 密集型的计算场景，理论上“线程的数量 =CPU 核数”就是最合适的。不过在工程上，线程的数量一般会设置为“CPU 核数 +1”.
- 于 I/O 密集型计算场景，最佳的线程数是与程序中 CPU 计算和 I/O 操作的耗时比相关的, 一个公式：最佳线程数 =1 +（I/O 耗时 / CPU 耗时）

对于多核CPU, 最佳线程数 =CPU 核数 * [ 1 +（I/O 耗时 / CPU 耗时）]

#### 为什么局部变量是线程安全的?

###### 为什么局部变量是线程安全的? 

那就需要先看下面几个问题了

###### 方法的调用在CPU层面是怎么进行的?

方法的执行需要有入参和返回地址, 这种东西存在CPU的什么地方? 那就是CPU的堆栈寄存器, 因为这个栈和方法有关, 常常被称为调用栈.
当调用方法的时候, 会创建一个栈帧, 这个栈帧就包含了该方法的局部变量和返回地址等, 创建的栈帧会进行压栈到调用栈, 多个栈帧在调用栈
上是相互不受影响的, 并且随着方法的结束而出栈, 也就是说栈帧与方法同生共死, 也就意味着局部变量与方法同生共死.

###### 局部变量是存在哪的?

局部变量是存在栈中的, 就如上面所说, 局部变量是创建在栈帧中, 进而存在在调用栈中. 这也就意味着变量如果想要跨越方法的边界, 就必须创建
在堆中.

那线程与调用栈的关系又是什么呢? 这个就很简单了, 每个线程有自己的调用栈, 互相不受影响, 独立.

###### 什么是线程封闭?

线程封闭其实就是说方法中局部变量不会和其他线程共享, 也就不会出现并发问题, 这成为解决并发问题的一个重要技术, 这个技术就叫线程封闭,
简单的说就是仅在单线程内访问数据.

###### 递归调用可能会产生栈溢出的情况, 为什么?

递归调用相当于调用多个方法, 每调用一次方法就会创建一个栈帧, 会进行压栈到调用栈中, 如果递归层次过多, 创建甚多栈帧进行压栈, 而
调用栈的内存不是无限大的, 总有容量, 当放不下的时候就会抛出栈溢出.

解决方案:
- 不适用递归, 使用循环(结构不清晰, 代码混乱)
- 限制递归次数
- 使用尾递归，尾递归是指在方法返回时只调用自己本身，且不能包含表达式。编译器或解释器会把尾递归做优化，使递归方法不论调用多少次，
都只占用一个栈帧，所以不会出现栈溢出。然鹅，Java没有尾递归优化。
 
#### 面向对象与并发结合

面向对象的思想让并发更加简单!!!

###### 封装共享变量

面向对象的一大特性就是封装, 封装就是将属性和实现细节封装在对象内部, 对外提供公共方法来间接访问. 这点用在并发领域就是将共享
变量封装在对象内部, 对所有公共方法制定并发访问策略.

对于不可变的共享变量, 声明为final类型, 也可以避免并发问题.

###### 识别共享变量间的约束条件

共享变量间的约束条件, 决定了并发访问策略, 举个简单例子, 库存.
```

public class SafeWM {
  // 库存上限
  private final AtomicLong upper =
        new AtomicLong(0);
  // 库存下限
  private final AtomicLong lower =
        new AtomicLong(0);
  // 设置库存上限
  void setUpper(long v){
    upper.set(v);
  }
  // 设置库存下限
  void setLower(long v){
    lower.set(v);
  }
  // 省略其他业务代码
}
```

库存使用了AtomicLong原子类, 所以现在不会出现并发问题, 但是忽略了一点, 库存下限要小于库存上限, 那简单加个if条件不行吗?
```

public class SafeWM {
  // 库存上限
  private final AtomicLong upper =
        new AtomicLong(0);
  // 库存下限
  private final AtomicLong lower =
        new AtomicLong(0);
  // 设置库存上限
  void setUpper(long v){
    // 检查参数合法性
    if (v < lower.get()) {
      throw new IllegalArgumentException();
    }
    upper.set(v);
  }
  // 设置库存下限
  void setLower(long v){
    // 检查参数合法性
    if (v > upper.get()) {
      throw new IllegalArgumentException();
    }
    lower.set(v);
  }
  // 省略其他业务代码
}
```

看到if能想到什么? 竞态条件, 程序的执行结果依赖于lower和upper的get值, 同样会产生并发问题.

那应该如何处理呢?
- 不考虑优化, 给setLower和setUpper加上synchronized锁
- 可以考虑将lower和upper进行封装, 封装共享变量, 然后设置的时候传入封装对象就可以

###### 制定并发访问策略

制定并发访问策略非常复杂, 但无外乎三件事:
- 避免共享: 避免共享技术主要是利于线程本地存储以及为每个任务分配独立的线程.
- 不变模式: Java领域少, 其他领域多. 例如Actor模式, CSP模式以及函数式编程的基础都是不变模式
- 管程及其它同步工具: Java领域万能的解决方案就是管程, 但是对于很多特定场景, 使用Java并发包提供的读写锁, 并发容器等同步工具更好.

三个宏观原则:
- 优先使用成熟的工具类：Java SDK 并发包里提供了丰富的工具类，基本上能满足你日常的需要，建议你熟悉它们，用好它们，而不是自己再“发明轮子”，
毕竟并发工具类不是随随便便就能发明成功的。
- 迫不得已时才使用低级的同步原语：低级的同步原语主要指的是 synchronized、Lock、Semaphore 等，这些虽然感觉简单，但实际上并没那么简单，
一定要小心使用。
- 避免过早优化：安全第一，并发程序首先要保证安全，出现性能瓶颈后再优化。在设计期和开发期，很多人经常会情不自禁地预估性能的瓶颈，
并对此实施优化，但残酷的现实却是：性能瓶颈不是你想预估就能预估的。


 
 