package com.concurrency.tool.read_write;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 使用读写锁实现缓存
 * @author 王智
 * @date  2020年3月23日
 */
public class ReadWriteCache<K, V> {

    /**
     * 存放缓存的Map
     */
    Map<K, V> map = new HashMap<>();

    /**
     * 可重入读写锁
     */
    final ReadWriteLock rwl = new ReentrantReadWriteLock();

    /**
     * 读锁
     */
    final Lock r =  rwl.readLock();

    /**
     * 写锁
     */
    final Lock w = rwl.writeLock();

    /**
     * 取值
     * @param k 键
     * @return
     */
    V get(K k) {
        r.lock();
        try {
            return map.get(k);
        } finally {
            r.unlock();
        }
    }

    /**
     * 写值
     * @param k 键
     * @param v 值
     */
    void put(K k, V v) {
        w.lock();
        try {
            map.put(k, v);
        } finally {
          w.unlock();
        }
    }

    /**
     * 按需初始化缓存
     * @param k
     * @return
     */
    V getInit(K k) {
        V v = null;
        r.lock();
        try {
            v = map.get(k);
        } finally {
            r.unlock();
        }
        if(v != null) {
            return v;
        }
        w.lock();
        try {
            // 为什么还要判断一次呢?
            // 原因就是防止多个线程并发重复写缓存
            v = map.get(k);
            if(v == null) {
                // 查询数据库获取V
                map.put(k, v);
            }
        } finally {
            w.unlock();
        }

        return v;
    }

}
