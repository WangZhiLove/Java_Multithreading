package com.multirhreading.atomicity;

/**
 * 使用互斥锁解决原子性问题
 * 前面说过锁与资源的关系应该是  1:N的关系  如果保护多个资源, 可以用一个锁, 也可以用多个锁
 *
 *
 * 原子性的本质是什么?  原子性的本质就是操作的中间状态对外不可见. 那解决原子性的方案就是保证操作的中间状态对外不可见
 *
 * @author  王智
 * @date   2020年3月19日
 */
public class SolveAtomicityTwo {



}

/**
 * 多个锁保护多个资源的情况, 也就是说用不同的锁对受保护的资源进行精细化管理, 这个就叫做细粒度锁
 *
 * 这里当然也可以使用Account.class或者静态的成员变量来作为锁来保护balance和password资源, 但是这种情况下整个操作都会成为串行,
 * 这样的话效率会差的很多
 *
 * todo 那可以用this.balance和this.password来作为锁保护对应的资源嘛?
 * 当然不可以, 不能用可变对象来作为锁保护资源, 这点非常重要
 */
class Account{

    private final Object balLock = new Object();

    private Integer balance;

    private final Object pwdLock = new Object();

    private String password;

    public void updateBalance(Integer num) {
        synchronized (balLock) {
            this.balance -= num;
        }
    }

    public Integer getBalance() {
        synchronized (balLock) {
            return balance;
        }

    }

    public void updatePassword(String password) {
        synchronized (pwdLock) {
            this.password = password;
        }
    }

    public String getPassword() {
        synchronized (pwdLock) {
            return password;
        }
    }
}

/**
 * 上面的情况是使用多个锁或一个锁保护没有关系的资源, 那如果要保护有关联的资源呢? 应该怎么处理呢?
 */
class AccountTransfer{

    private Integer account;

    /**
     * 这种加锁的方法可以吗?  用一把锁保护有关联的两个资源!!!
     * 当然是不可以的, 这里synchronized锁的是this对象, 而资源是当前对象的account和目标对象account, 当前的this锁
     * 只能锁住当前对象的account, 而不能锁住,目标的account.
     *
     * todo 那应该怎么做呢? 其实也简单, 我们可以使用一个大锁来报, 也就是AccountTransfer.class
     */
    public synchronized void transfer(Integer num, AccountTransfer target) {
        this.account -= num;
        target.account += num;
    }

    /**
     * 这种确实可以解决原子性问题, 但是性能确是格外的差, 原因是所有的操作都变成了串行, 都阻塞了就会导致性能格外的差
     */
    public void transferLock(Integer num, AccountTransfer target) {
        synchronized (AccountTransfer.class) {
            this.account -= num;
            target.account += num;
        }
    }
}
