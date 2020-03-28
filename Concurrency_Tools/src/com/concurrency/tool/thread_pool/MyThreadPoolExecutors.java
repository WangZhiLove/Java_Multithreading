package com.concurrency.tool.thread_pool;

import com.sun.org.apache.bcel.internal.generic.NEWARRAY;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 模拟线程池的工作原理
 * @author  王智
 * @date   2020年3月28日
 */
public class MyThreadPoolExecutors {

    /**
     * 生产者消费者阻塞队列
     */
    BlockingQueue<Runnable> workQueue;

    /**
     * 内部工作线程组
     */
    List<WorkThread> threads = new ArrayList<>();


    public MyThreadPoolExecutors(BlockingQueue<Runnable> workQueue, Integer poolSize) {
        this.workQueue = workQueue;
        // 创建工作线程
        for (Integer i = 0; i < poolSize; i++) {
            WorkThread workThread = new WorkThread();
            workThread.start();
            threads.add(workThread);
        }
    }


    /**
     * 提交任务
     * @param command
     */
    void execute(Runnable command) throws InterruptedException {
        workQueue.put(command);
    }

    /**
     * 工作线程负责消费任务, 并执行任务
     */
    class WorkThread extends Thread{
        @Override
        public void run() {
            // 提起任务并执行
            while(true) {
                Runnable take = null;
                try {
                    take = workQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                take.run();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {

        //无论是工作线程组的大小还是有界阻塞队列的大小, 都要根据场景和实际情况来决定
        BlockingQueue workQueue = new LinkedBlockingDeque(4);
        MyThreadPoolExecutors pool = new MyThreadPoolExecutors(workQueue, 10);
        // 提交任务
        pool.execute(() -> {
            System.out.println("Hello pool");
        });

    }
}
