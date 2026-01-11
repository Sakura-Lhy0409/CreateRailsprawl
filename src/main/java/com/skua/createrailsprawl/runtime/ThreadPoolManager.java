package com.skua.createrailsprawl.runtime;

import com.skua.createrailsprawl.RailwayConfig;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程池管理器 - 借鉴 RoadWeaver 的优化
 */
public final class ThreadPoolManager {
    private ThreadPoolManager() {}

    private static volatile ExecutorService COMPUTE_EXEC;
    private static final AtomicLong EPOCH = new AtomicLong(0L);
    private static final ThreadLocal<Long> WORK_START = ThreadLocal.withInitial(System::currentTimeMillis);
    private static final long WORK_PERIOD_MS = 20;

    private static ThreadFactory namedFactory(String prefix) {
        return r -> {
            Thread t = new Thread(r, prefix + "-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        };
    }

    public static synchronized void onServerStarted(MinecraftServer server) {
        EPOCH.incrementAndGet();
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        if (COMPUTE_EXEC != null && !COMPUTE_EXEC.isShutdown()) {
            try { COMPUTE_EXEC.shutdownNow(); } catch (Throwable ignored) {}
        }
        COMPUTE_EXEC = Executors.newFixedThreadPool(threads, namedFactory("CRS-Compute"));
    }

    public static synchronized void onServerStopping() {
        EPOCH.incrementAndGet();
        if (COMPUTE_EXEC != null) {
            try { COMPUTE_EXEC.shutdownNow(); } catch (Throwable ignored) {}
            COMPUTE_EXEC = null;
        }
    }

    public static ExecutorService computeExecutor() {
        ExecutorService e = COMPUTE_EXEC;
        if (e == null || e.isShutdown()) {
            synchronized (ThreadPoolManager.class) {
                if (COMPUTE_EXEC == null || COMPUTE_EXEC.isShutdown()) {
                    int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
                    COMPUTE_EXEC = Executors.newFixedThreadPool(threads, namedFactory("CRS-Compute"));
                }
                e = COMPUTE_EXEC;
            }
        }
        return e;
    }

    public static long currentEpoch() {
        return EPOCH.get();
    }

    public static boolean isEpoch(long epoch) {
        return EPOCH.get() == epoch;
    }

    /**
     * 节流检查点 - 在耗时任务的循环中周期性调用
     * 根据占空比，工作一段时间后主动休眠，避免卡顿
     */
    public static void throttle(int dutyCycle) {
        if (dutyCycle >= 100) return;
        if (dutyCycle <= 0) dutyCycle = 50;

        long now = System.currentTimeMillis();
        long elapsed = now - WORK_START.get();

        if (elapsed >= WORK_PERIOD_MS) {
            long sleepMs = (long) (WORK_PERIOD_MS * (100.0 - dutyCycle) / dutyCycle);
            if (sleepMs > 0) {
                try {
                    Thread.sleep(Math.min(sleepMs, 200));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            WORK_START.set(System.currentTimeMillis());
        }
    }

    public static void resetThrottle() {
        WORK_START.set(System.currentTimeMillis());
    }

    public static void clearThrottle() {
        WORK_START.remove();
    }
}
