package com.multirhreading.state;

/**
 * 线程状态的判断
 */
public class StateDemo {
    public static void main(String[] args) {

        for (int i = 0; i < 10; i++) {
           new Thread(() -> {
                    System.out.println("12345678");
               try {
                   Thread.sleep(60000);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }).start();
        }
    }
}
