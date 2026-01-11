package com.skua.createrailsprawl.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 计算服务工具类 - 封装异步任务提交
 */
public final class ComputeService {
    private ComputeService() {}

    /**
     * 异步执行有返回值的任务
     */
    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, ThreadPoolManager.computeExecutor());
    }

    /**
     * 异步执行无返回值的任务
     */
    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, ThreadPoolManager.computeExecutor());
    }

    /**
     * 带 Epoch 检查的异步任务
     */
    public static <T> CompletableFuture<T> supplyAsyncWithEpoch(Supplier<T> supplier) {
        final long epoch = ThreadPoolManager.currentEpoch();
        return CompletableFuture.supplyAsync(() -> {
            if (!ThreadPoolManager.isEpoch(epoch)) return null;
            return supplier.get();
        }, ThreadPoolManager.computeExecutor());
    }

    /**
     * 带 Epoch 检查的异步任务（无返回值）
     */
    public static CompletableFuture<Void> runAsyncWithEpoch(Runnable runnable) {
        final long epoch = ThreadPoolManager.currentEpoch();
        return CompletableFuture.runAsync(() -> {
            if (!ThreadPoolManager.isEpoch(epoch)) return;
            runnable.run();
        }, ThreadPoolManager.computeExecutor());
    }
}
