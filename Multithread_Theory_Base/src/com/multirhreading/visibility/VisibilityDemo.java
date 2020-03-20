package com.multirhreading.visibility;

/**
 * 多线程之可见性的Bug, 可见性问题在单CPU情况下是不会出现的, 会出现的是多CPU的缓存下, 多线程出现的问题
 * @author  王智
 * @date  2020年3月10日
 */
public class VisibilityDemo {

    private long count = 0;

    private void add100k() {
        int num = 0;
        while(num ++ < 100000) {
            count ++;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final VisibilityDemo visibilityDemo = new VisibilityDemo();
        // 创建线程 TODO 多核CPU多线程, 共享变量不可见, 导致出现的Bug
        Thread thread1 = new Thread(() -> {
            visibilityDemo.add100k();
        });
        Thread thread2 = new Thread(() -> {
            visibilityDemo.add100k();
        });
        // 启动线程
        thread1.start();
        thread2.start();
        // 等待线程结束
        thread1.join();
        thread2.join();
        System.out.println(visibilityDemo.count);
    }

}
