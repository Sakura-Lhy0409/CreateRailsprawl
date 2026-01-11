package com.skua.createrailsprawl.runtime;

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
    private static volatile ExecutorService GENERATION_EXEC;
    private static final AtomicLong EPOCH = new AtomicLong(0L);
    private static final ThreadLocal<Long> WORK_START = ThreadLocal.withInitial(System::currentTimeMillis);
    private static final long WORK_PERIOD_MS = 20;

    // 默认线程数配置
    private static int computeThreads = 0;  // 0 = 自动 (CPU-1)
    private static int generationThreads = 4;

    private static ThreadFactory namedFactory(String prefix) {
        return r -> {
            Thread t = new Thread(r, prefix + "-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        };
    }

    public static synchronized void onServerStarted(MinecraftServer server) {
        EPOCH.incrementAndGet();

        // 计算线程池
        int cThreads = resolveComputeThreads();
        if (COMPUTE_EXEC != null && !COMPUTE_EXEC.isShutdown()) {
            try { COMPUTE_EXEC.shutdownNow(); } catch (Throwable ignored) {}
        }
        COMPUTE_EXEC = Executors.newFixedThreadPool(cThreads, namedFactory("CRS-Compute"));

        // 生成线程池
        int gThreads = Math.max(1, generationThreads);
        if (GENERATION_EXEC != null && !GENERATION_EXEC.isShutdown()) {
            try { GENERATION_EXEC.shutdownNow(); } catch (Throwable ignored) {}
        }
        GENERATION_EXEC = Executors.newFixedThreadPool(gThreads, namedFactory("CRS-Gen"));
    }

    public static synchronized void onServerStopping() {
        EPOCH.incrementAndGet();
        if (COMPUTE_EXEC != null) {
            try { COMPUTE_EXEC.shutdownNow(); } catch (Throwable ignored) {}
            COMPUTE_EXEC = null;
        }
        if (GENERATION_EXEC != null) {
            try { GENERATION_EXEC.shutdownNow(); } catch (Throwable ignored) {}
            GENERATION_EXEC = null;
        }
    }

    public static ExecutorService computeExecutor() {
        ExecutorService e = COMPUTE_EXEC;
        if (e == null || e.isShutdown()) {
            synchronized (ThreadPoolManager.class) {
                if (COMPUTE_EXEC == null || COMPUTE_EXEC.isShutdown()) {
                    COMPUTE_EXEC = Executors.newFixedThreadPool(resolveComputeThreads(), namedFactory("CRS-Compute"));
                }
                e = COMPUTE_EXEC;
            }
        }
        return e;
    }

    public static ExecutorService generationExecutor() {
        ExecutorService e = GENERATION_EXEC;
        if (e == null || e.isShutdown()) {
            synchronized (ThreadPoolManager.class) {
                if (GENERATION_EXEC == null || GENERATION_EXEC.isShutdown()) {
                    GENERATION_EXEC = Executors.newFixedThreadPool(Math.max(1, generationThreads), namedFactory("CRS-Gen"));
                }
                e = GENERATION_EXEC;
            }
        }
        return e;
    }

    /**
     * 运行时调整计算线程池大小
     */
    public static synchronized void resizeComputePool(int threads) {
        computeThreads = threads;
        int cThreads = resolveComputeThreads();
        if (COMPUTE_EXEC != null && !COMPUTE_EXEC.isShutdown()) {
            try { COMPUTE_EXEC.shutdownNow(); } catch (Throwable ignored) {}
        }
        COMPUTE_EXEC = Executors.newFixedThreadPool(cThreads, namedFactory("CRS-Compute"));
    }

    /**
     * 运行时调整生成线程池大小
     */
    public static synchronized void resizeGenerationPool(int threads) {
        generationThreads = threads;
        int gThreads = Math.max(1, threads);
        if (GENERATION_EXEC != null && !GENERATION_EXEC.isShutdown()) {
            try { GENERATION_EXEC.shutdownNow(); } catch (Throwable ignored) {}
        }
        GENERATION_EXEC = Executors.newFixedThreadPool(gThreads, namedFactory("CRS-Gen"));
    }

    private static int resolveComputeThreads() {
        if (computeThreads > 0) {
            return computeThreads;
        }
        return Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
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
