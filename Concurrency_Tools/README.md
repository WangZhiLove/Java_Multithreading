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

#### Condition

Java SDK中的lock与synchronized的不同是提供了三个可以可以中断死锁的特性(响应中断, 支持超时和非阻塞获取锁), 
而Condition与synchronized的不同在于Condition支持多条件变量, synchronized只支持一个条件变量.

##### 两个条件实现阻塞队列

这个两个条件就是出队队列不可以为空, 入队队列不能满, 前面已经站试过了.
```

public class BlockedQueue<T>{
  final Lock lock =
    new ReentrantLock();
  // 条件变量：队列不满  
  final Condition notFull =
    lock.newCondition();
  // 条件变量：队列不空  
  final Condition notEmpty =
    lock.newCondition();

  // 入队
  void enq(T x) {
    lock.lock();
    try {
      while (队列已满){
        // 等待队列不满
        notFull.await();
      }  
      // 省略入队操作...
      //入队后,通知可出队
      notEmpty.signal();
    }finally {
      lock.unlock();
    }
  }
  // 出队
  void deq(){
    lock.lock();
    try {
      while (队列已空){
        // 等待队列不空
        notEmpty.await();
      }  
      // 省略出队操作...
      //出队后，通知可入队
      notFull.signal();
    }finally {
      lock.unlock();
    }  
  }
}
```

注意的是不要把Lock/Condition和隐式锁的等待通知搞混, Lock/condition的就是await(), signal()和signalAll(),
隐式锁就是wait(), notify()和notifyAll(). 一旦混用, 程序就彻底完了.

###### 同步和异步

同步的概念就是调用方需要等待被调用方返回的结果, 异步的概念就是调用方不需要等待结果.

异步的两种实现方式:
- 在主线程中声明子线程, 在子线程中执行方法调用, 这就是异步调用
- 方法实现的之后, 创建一个线程执行主要逻辑, 主线程直接return, 这就是异步方法(我觉得赢了happen-Before的start()) 

在TCP层面, 发送完RPC请求之后, 线程是不会等待RPC请求返回的结果的, 但是工作中的RPC大多都是同步的, 原因就是框架将RPC
调用的异步转化为同步, 这个转化就是利用了Lock/Condition.

```

// 创建锁与条件变量
private final Lock lock 
    = new ReentrantLock();
private final Condition done 
    = lock.newCondition();

// 调用方通过该方法等待结果
Object get(int timeout){
  long start = System.nanoTime();
  lock.lock();
  try {
  while (!isDone()) {
    done.await(timeout);
      long cur=System.nanoTime();
    if (isDone() || 
          cur-start > timeout){
      break;
    }
  }
  } finally {
  lock.unlock();
  }
  if (!isDone()) {
  throw new TimeoutException();
  }
  return returnFromResponse();
}
// RPC结果是否已经返回
boolean isDone() {
  return response != null;
}
// RPC结果返回时调用该方法   
private void doReceived(Response res) {
  lock.lock();
  try {
    response = res;
    if (done != null) {
      done.signalAll();
    }
  } finally {
    lock.unlock();
  }
}
```

#### 信号量实现限流器

##### 信号量模型

Semaphore, 信号量, 信号量是在管程之前提出的, 比管程的历史还要久一点, 在管程出现之前, 并发问题就一直是使用
信号量来解决的.

信号量模型的组成包括了: 一个计数器, 一个等待队列, 三个方法(init(), down(), up())方法, 计数器计数所用, 
等待队列用来存放阻塞线程的, init()方法是初始化计数器的;  down()进行计数器减一操作, 当计数器小于0的时候, 
将线程放入阻塞队列, 否则线程继续执行; up()进行计数器加一的操作, 当计数器<=0, 唤醒阻塞队列的一个线程, 并将其
移出阻塞队列. 具体的代码可以看下面:

```

class Semaphore{
  // 计数器
  int count;
  // 等待队列
  Queue queue;
  // 初始化操作
  Semaphore(int c){
    this.count=c;
  }
  // 
  void down(){
    this.count--;
    if(this.count<0){
      //将当前线程插入等待队列
      //阻塞当前线程
    }
  }
  void up(){
    this.count++;
    if(this.count<=0) {
      //移除等待队列中的某个线程T
      //唤醒线程T
    }
  }
}
```

信号量模型里面, down()和up()最早被称为PV操作, 所以信号量模型最早也叫做PV原语. 在Java SDK中,
down()和up()对应的是acquire()和release(), 使用信号量模型实现计数器 - SemaphoreModelCount

##### 使用信号量模型实现限流器

SemaphoreModelCount中的案例是使用信号量模型来实现互斥锁的功能, 如果仅仅是这些功能, 那个Lock
有什么区别呢? 所以信号量模型的特别之处在于信号量模型**允许多个线程访问同一个临界区**, 最常见的就是
数据库连接池等, 在不释放之前, 其他线程是无法访问的. 

下面用信号量实现一个对象池, 实现限流, 也就是说创建一个放了N个对象的对象池, 同一时刻不能有多于N个对象来进入临界区.

CurrentLimiter.java类中可以看到.

到这里, 知道Java实现管程的两种方式:
- 隐式锁
- Lock和Condition

不同之处在于前者只支持单条件变量, 后者支持多条件变量.

管程和信号量的区别在于: 管程只支持一个线程进入临界区, 而信号量模型则支持多个线程同时进入临界区.

















