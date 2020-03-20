package com.multirhreading.memory.model;

/**
 * volatile关键字, volatile关键字的目的是禁用缓存, 告诉编译器对这个变量的读写需要从内存中读写, 不能使用缓存
 * 暂时模拟不出这种情况, 但是这种情况是真实存在的
 * @author  王智
 * @date    2020年3月12日
 */
public class VolatileDemo {

    int x = 0;
    boolean y = false;
    int k = 0;
    volatile boolean z = false;

    public void readXAndY(String name) {
        if(y == true) {
            System.out.println(name + " x111 = " + x);
            System.out.println(name + " k111 = " + k);
        }
        // System.out.println(name + " .......x = " + x);

    }

    public void writeXAndY() {
        x = 42;
        y = true;
        k = 36;
    }

    public void readKAndZ(String name) {
        if(z == true) {
            System.out.println(name + " k = " + k);
        }
        // System.out.println(name + " .......k = " + k);
    }

    public void writeKAndZ() {
        k = 42;
        z = true;
    }

    public static void main(String[] args) {
        /*for (int i = 0; i < 100; i++) {
            VolatileDemo volatileDemo = new VolatileDemo();
            volatileDemo.writeXAndY();
            volatileDemo.readXAndY();
            volatileDemo.writeKAndZ();
            volatileDemo.readKAndZ();
        }*/
        for (int i = 0; i < 10000; i++) {
            VolatileDemo volatileDemo = new VolatileDemo();
            Thread thread1 = new Thread(() -> {
                volatileDemo.writeXAndY();
            });
            Thread thread2 = new Thread(() -> {
                volatileDemo.readXAndY(Thread.currentThread().getName());
            });
            Thread thread3 = new Thread(() -> {
                volatileDemo.writeKAndZ();
            });
            Thread thread4 = new Thread(() -> {
                volatileDemo.readKAndZ(Thread.currentThread().getName());
            });
            thread2.start();
            thread1.start();
            //thread3.start();
            //thread4.start();
        }
    }

}
