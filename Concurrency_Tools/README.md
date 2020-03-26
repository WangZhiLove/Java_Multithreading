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

#### 读写锁实现缓存

##### 读写锁

Java SDK中的工具类, 更多是为了 分场景优化性能, 提升易用性.

ReadWriteLock读写锁, 遵循三个基本原则:
- 允许多个线程同时读共享变量
- 只允许一个线程写共享变量
- 当有线程写共享变量的时候, 阻塞其他读共享变量的线程

读写锁和互斥锁的区别在于读写锁允许多个线程同时读共享变量, 但是读锁和写锁之间是互斥的.

##### 实现缓存

ReadWriteCache.java实现了缓存的读和写, 缓存的初始化如何实现?

缓存的初始化有两种方式:
- 一次性写入
- 按需写入

一次性写入适合的情况就是数据量少的情况, 如果数据量非常大, 那就最好使用按需写入, 需要用到的时候
检查缓存中有没有值, 有值, 直接读; 没有值, 先写后读. - ReadWriteCache.java中的getInit()方法.

##### 读写锁的升级和降级

这里要记住的是读写锁只存在降级, 不存在升级.
```

//读缓存
r.lock();         ①
try {
  v = m.get(key); ②
  if (v == null) {
    w.lock();
    try {
      //再次验证并更新缓存
      //省略详细代码
    } finally{
      w.unlock();
    }
  }
} finally{
  r.unlock();     ③
}
```
像这种锁的升级是不允许的, 会造成写锁永久等待, 进而倒是其他线程全部阻塞. 但锁的降级确是允许的
```

class CachedData {
  Object data;
  volatile boolean cacheValid;
  final ReadWriteLock rwl =
    new ReentrantReadWriteLock();
  // 读锁  
  final Lock r = rwl.readLock();
  //写锁
  final Lock w = rwl.writeLock();
  
  void processCachedData() {
    // 获取读锁
    r.lock();
    if (!cacheValid) {
      // 释放读锁，因为不允许读锁的升级
      r.unlock();
      // 获取写锁
      w.lock();
      try {
        // 再次检查状态  
        if (!cacheValid) {
          data = ...
          cacheValid = true;
        }
        // 释放写锁前，降级为读锁
        // 降级是可以的
        r.lock(); ①
      } finally {
        // 释放写锁
        w.unlock(); 
      }
    }
    // 此处仍然持有读锁
    try {use(data);} 
    finally {r.unlock();}
  }
}
```

上面的缓存初始化就完成了, 但是还有一点, 就是缓存与数据源的同步问题, 同样两种方案:
- 在数据源修改的时候同步到缓存
- 超时机制, 规定时长, 超出时间去同步.

还有注意读写锁中的读锁是不支持条件变量的, 写锁支持条件变量, 也就是不能用读锁去newCondition, 
会直接抛出异常. 但是可以用写锁去newCondition, 原因是读写锁的读锁是不会产生互斥的.

#### StampedLock - 读写锁的又一种实现

ReadWriteLock读写锁的速度已经很快了, 但是在Java8中, 又加了一个StampedLock, 也是一种读写锁,
速度优于ReadWriteLock, 如何实现呢? StampedLock是将读锁分为了两种: 乐观读与悲观读锁. 悲观读锁
和写锁与ReadWriteLock的读写锁语义基本相同, 不同之处在于StampedLock的写锁与悲观读锁加锁成功后
返回一个stamp, 解锁的时候需要传这个stamp. 使用方面的加锁与释放锁如类 StampedLockDemo.java.

StampedLock性能好的地方在于ReadWriteLock在加了读锁之后, 所有的写锁都会阻塞.但是stampedLock的
乐观读会允许一个线程获取写锁, 也就是并不是所有的写锁都会阻塞.

要说的是乐观读是无锁操作, tryOptimisticRead()方法就是获取stamp, 也就是乐观读. 要验证stamp有没有修改,
需要用到validate(stamp)方法.

##### MySQL的乐观锁

MySQL的乐观锁如何实现的呢? MySQL就是加了个version字段, 每次在进行读的时候讲version字段查出来进行返回,
在进行写的时候讲version字段加1, 然后在修改更新的时候用version字段做个校验.

##### StampedLock使用的注意事项

- StampedLock是ReadWriteLock的子集
- StampedLock不支持重入
- StampedLock的写锁和悲观读锁不支持条件变量, 不能响应中断
- 使用 StampedLock 一定不要调用中断操作，如果需要支持中断功能，一定使用可中断的悲观读锁 
readLockInterruptibly() 和写锁 writeLockInterruptibly()。

##### StampedLock使用模板

读模板

```

final StampedLock sl = 
  new StampedLock();

// 乐观读
long stamp = 
  sl.tryOptimisticRead();
// 读入方法局部变量
......
// 校验stamp
if (!sl.validate(stamp)){
  // 升级为悲观读锁
  stamp = sl.readLock();
  try {
    // 读入方法局部变量
    .....
  } finally {
    //释放悲观读锁
    sl.unlockRead(stamp);
  }
}
//使用方法局部变量执行业务操作
```

写模板
```
long stamp = sl.writeLock();
try {
  // 写共享变量
  ......
} finally {
  sl.unlockWrite(stamp);
}
```

#### CountDownLatch和CyclicBarrier: 多线程步调一致

CountDownLatch的使用场景是一个线程等待多个线程的情况.

CyclicBarrier的使用场景是多个线程互相等待的情况.

也就是说CountDownLatch是一个线程等待其他多个线程都执行完成, 才开始执行.

CyclicBarrier是多个线程互相等待, 就是多个线程都执行完才向下执行.看着效果相同, 使用场景还是有区别的.

##### 对账系统案例

对账系统的逻辑是先查询订单, 再查询派送单, 然后对比差异, 将差异保存到差异表.代码如下:

```

while(存在未对账订单){
  // 查询未对账订单
  pos = getPOrders();
  // 查询派送单
  dos = getDOrders();
  // 执行对账操作
  diff = check(pos, dos);
  // 差异写入差异库
  save(diff);
} 
```
就目前来看, 这一系列的操作都是串行的, 优化的空间有很大, 如何做呢? 首先将操作进行分析, 看看各个操作
之间的关系, 查询未对账订单金额查询派送单是没有先后顺序的, 可以并行, 对账操作必须在获取未对账订单和派送单
之后, 对账操作和写入差异库必须是串行的.所以可以用多线程来达到并行的目的.
```
while(存在未对账订单){
    Thread t1 = new Thread(() -> {
        // 查询未对账订单
        pos = getPOrders();
    });
    Thread t2 = new Thread(() -> {
        // 查询派送单
        dos = getDOrders();
    });
    t1.start();
    t2.start();
    // 等待t1和t2结束
    t1.join();
    t2.join();
    // 执行对账操作
    diff = check(pos, dos);
    // 差异写入差异库
    save(diff);
}
```
这样就可以了, 但是在while循环里面每次都要创建线程, 这个是很耗资源的, 用线程池的话应该会更好些.
但是用线程池的话join()操作就没办法使用了, 那如何实现两个线程执行完之后再执行对账操作呢?
```
// 创建两个线程的线程池
Executor executor = Executors.newFixedThreadPool(2);
while(存在未对账订单){
   exector.execute(() -> {
        // 查询未对账订单
        pos = getPOrders();
    });
    executor.execute(() -> {
        // 查询派送单
        dos = getDOrders();
    });
    // 如何来等待两个线程结束
    ********************
    // 执行对账操作
    diff = check(pos, dos);
    // 差异写入差异库
    save(diff);
}
```

##### CountDownLatch实现线程池一个线程等待多线程

这里的目的是一个线程等待多个线程完成, 那可以使用CountDownLatch, 那如何使用?
```
// 创建两个线程的线程池
Executor executor = Executors.newFixedThreadPool(2);
// 创建CountDownLatch, 计数器初始化是2, 也就是等待两个线程
CountDownLatch cdl = new CountDownLatch(2);
while(存在未对账订单){
   exector.execute(() -> {
        // 查询未对账订单
        pos = getPOrders();
        cdl.countDown();
    });
    executor.execute(() -> {
        // 查询派送单
        dos = getDOrders();
        cdl.countDown();
    });
    // 等待两个线程执行结束
    cdl.await();
    // 执行对账操作
    diff = check(pos, dos);
    // 差异写入差异库
    save(diff);
}
```
countDown()方法来使得计数器减一, await()方法是来实现对计数器等于0的等待.现在的程序
性能相对于之前更好了些. 但是还有可优化的地方, 那就是查询订单和对账操作之间是串行的, 也就是
查询订单的时候不能对账, 对账的时候不能查询订单, 这两个操作其实是可以并行的, 也就是第一次查询完订单
之后, 在对账期间, 可以查询接下来的订单, 这就有点像生产者和消费者了, 查询订单是生产者, 对账
操作是消费者, 那生产者和消费者处理就是队列呗.那使用队列的话如何能保证两个查询订单的操作同时完成呢?
这里就要用到CyclicBarrier来实现线程同步了.                                        

##### CyclicBarrier来实现线程相互等待同步

使用 CyclicBarrier 来实现多个线程之间相互等待, 同步完成. 使用 CyclicBarrier 的时候需要传递
一个回调函数.并且  CyclicBarrier 计数器拥有重置的功能, 当减为0的时候, 会重置为设置的值.
```
// 订单队列
Vector<P> pos;
// 派送队列
Vector<D> dos;
// 一个线程池, 执行回调函数的线程池, 因为指向回调函数不会重新开一个线程.
Executor executor = Executors.new FixedThreadPool(1);
// 创建同步对象, 第一个参数是计数器, 第二个参数是回调函数
final CycliBarrier cycliBarrier = new CycliBarrier(2, () -> {
    executor.execute(() -> check());
});
void check() {
    P p = pos.remove(0);
    D d = dos.remove(0);
    // 执行对账操作
    diff = check(p, d);
    // 差异写入差异库
    save(diff);
} 
void checkAll() {
    // 查询订单库
    Thread t1 = new Thraed(() - > {
        // 入队
        pos.add(getPOrders());
        // 等待
        cycliBarrier.await();
    });
    // 查询派送单
    Thread t2 = new Thraed(() - > {
        // 入队
        dos.add(getDOrders());
        // 等待
        cycliBarrier.await();
    });
    t1.start();
    t2.start();
}
```

由于是单线程执行对账操作, 所以整个流程下来是没问题, 多线程的话可能就会存在很大的问题, 需要
将方法加锁.

使用CountDownLatch可以保证一个线程等待多个线程的场景.

CyclicBarrier可以保证线程之间相互等待的场景.并且该计数器是可以重置的, 这个很重要.















