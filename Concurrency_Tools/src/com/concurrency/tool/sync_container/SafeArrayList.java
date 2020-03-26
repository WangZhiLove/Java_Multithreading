package com.concurrency.tool.sync_container;

import java.util.*;

/**
 * 将ArrayList变成同步
 * @author  王智
 * @date  2020年3月26日
 */
public class SafeArrayList<T> {

    /**
     * 不安全的List, 这个时候只需要将所有操作都加上隐式锁, 那就可以保证线程安全了.
     */
    List<T> list = new ArrayList<>();

    synchronized T get(int index) {
        return list.get(index);
    }

    synchronized void put(T t) {
        list.add(t);
    }

    /**
     * 判断不存在再添加
     * @param t
     * @return
     */
    synchronized boolean ifNotExists(T t) {
        // 组合操作, 即使能保证每一个操作都是原子性的, 安全的, 但是也不能完全避免并发问题, 原因是存在竞态条件.
        if(!list.contains(t)) {
            list.add(t);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        Collections.synchronizedList(new ArrayList<>());
        Collections.synchronizedMap(new HashMap<>());
        Collections.synchronizedSet(new HashSet<>());

    }

}
