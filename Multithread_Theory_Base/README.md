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