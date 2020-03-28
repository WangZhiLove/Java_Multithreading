package com.concurrency.tool.future;

import java.util.concurrent.*;

/**
 * 使用FutureTask来实现烧水的最佳工序
 * @author  王智
 * @date  2020年3月28日
 */
public class FutureTaskDemo {

    public static void main(String[] args) {
        // 创建两个线程, 任务1和任务2
        FutureTask<String> futureTask2 = new FutureTask<>(new Task2());
        FutureTask<String> futureTask1 = new FutureTask<>(new Task1(futureTask2));
        // 创建阻塞队列
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(2);
        // 创建线程池
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                2,
                2,
                1,
                TimeUnit.HOURS,
                queue,
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("烧水");
                        return t;
                    }
                });
        threadPoolExecutor.submit(futureTask1);
        threadPoolExecutor.submit(futureTask2);
    }

}

/**
 * 执行洗茶壶, 烧水, 泡茶
 */
class Task1 implements Callable<String> {
    FutureTask<String> futureTask;

    public Task1(FutureTask<String> futureTask) {
        this.futureTask = futureTask;
    }

    @Override
    public String call() throws Exception {
        System.out.println("T1: 洗水壶");
        TimeUnit.SECONDS.sleep(1);

        System.out.println("T1: 烧开水");
        TimeUnit.SECONDS.sleep(10);

        String result = futureTask.get();
        System.out.println("T1: 拿到茶叶: " + result);

        System.out.println("T1: 泡茶");
        return "上茶:" + result;
    }
}

/**
 * 执行洗茶壶, 洗茶杯, 拿茶操作
 */
class Task2 implements Callable<String> {

    @Override
    public String call() throws Exception {
        System.out.println("T2: 洗茶壶");
        TimeUnit.SECONDS.sleep(1);

        System.out.println("T2: 洗茶杯");
        TimeUnit.SECONDS.sleep(1);

        System.out.println("T2: 取茶叶");
        return "龙井茶";
    }
}