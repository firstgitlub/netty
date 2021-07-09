package io.netty.example.socksproxy;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class TestFuture {
    static class BoilWater implements Callable<String> {
        public String call() throws Exception {
            Thread.sleep(5000);            // 模拟一个耗时操作
            return System.currentTimeMillis() + " | 热水器：主人，水烧开了";   //烧水完成
        }
    }

    static class CallFutureTask extends FutureTask<String>{

        private Callable callable1;

        public CallFutureTask(Callable<String> callable,Callable<String> callableBack1) {
            super(callable);
            callable1 = callableBack1;
        }

        @Override
        protected void done() {
            super.done();

            try {
                Object ss = callable1.call();
                System.out.println("====="+ss.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class CallBackCallable implements Callable<String> {
        public String call() throws Exception {
            return System.currentTimeMillis() + " | 热水器：主人，水烧开了";   //烧水完成
        }
    }

    public static void main(String[] args) throws Exception {

        CallFutureTask callFutureTask = new CallFutureTask(new BoilWater(),new CallBackCallable());
        new Thread(callFutureTask).start();
        System.out.println(System.currentTimeMillis() + " | 我：小热，你可以开始烧水了...");

//        //创建一个线程池，相当于找到一个热水器
//        FutureTask futureTask = new FutureTask<>(new BoilWater());
//        // 开始烧水
//        // Executors.newCachedThreadPool().submit(futureTask); 这样使用线程池的形式，
//        // 结果是一样的
//        new Thread(futureTask).start();
//        System.out.println(System.currentTimeMillis() + " | 我：小热，你可以开始烧水了...");
//
//        System.out.println((String) futureTask.get());
//
//        // 继续打Dota
//        System.out.println(System.currentTimeMillis() + " | 我：继续开心地玩Dota...");
//        Thread.sleep(4000);
//        System.out.println(System.currentTimeMillis() + " | 我：Oh，我死了...");
//
//        // 被干了或者上对面高地了，有时间来检查水是否烧好了。
//        System.out.println(System.currentTimeMillis() + " | 我：看看水烧好没...");
//        while (!futureTask.isDone()) {
//            System.out.println(System.currentTimeMillis() + " | 我：水还没烧开，小热，你快点烧，我再给你一秒钟...");
//            Thread.sleep(1000);
//        }
//        // 使用get函数来获取烧水结果
//        System.out.println((String) futureTask.get());
    }
}
