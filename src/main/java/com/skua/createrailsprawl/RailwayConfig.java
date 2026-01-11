package com.skua.createrailsprawl;

import com.skua.createrailsprawl.runtime.ThreadPoolManager;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = CreateRailsprawl.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RailwayConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 基础功能配置
    private static final ForgeConfigSpec.BooleanValue ENABLE_TRACK_SPAWNER = BUILDER
            .comment("Can the track spawner work")
            .define("enableTrackSpawner", true);

    private static final ForgeConfigSpec.BooleanValue GENERATE_TRACK_SPAWNER = BUILDER
            .comment("Can the track spawner be generated")
            .define("generateTrackSpawner", true);

    private static final ForgeConfigSpec.BooleanValue PLACE_TRACKS_USING_TRACK_SPAWNER = BUILDER
            .comment("Use the track spawner,(true) or place rails during world generation.(false)")
            .define("placeTracksUsingTrackSpawner", true);

    // 线程配置
    private static final ForgeConfigSpec.IntValue COMPUTE_THREADS = BUILDER
            .comment("Number of compute threads (0 = auto, CPU cores - 1)")
            .defineInRange("computeThreads", 0, 0, 64);

    private static final ForgeConfigSpec.IntValue GENERATION_THREADS = BUILDER
            .comment("Number of generation threads")
            .defineInRange("generationThreads", 4, 1, 64);

    private static final ForgeConfigSpec.IntValue THREAD_DUTY_CYCLE = BUILDER
            .comment("Thread duty cycle percentage (1-100), lower = less CPU usage")
            .defineInRange("threadDutyCycle", 50, 1, 100);

    // A* 寻路权重配置
    private static final ForgeConfigSpec.DoubleValue ELEVATION_WEIGHT = BUILDER
            .comment("Weight for elevation changes in pathfinding")
            .defineInRange("elevationWeight", 2.0, 0.1, 10.0);

    private static final ForgeConfigSpec.DoubleValue HEURISTIC_WEIGHT = BUILDER
            .comment("Heuristic weight for A* pathfinding")
            .defineInRange("heuristicWeight", 1.0, 0.1, 5.0);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // 基础功能
    public static boolean enableTrackSpawner;
    public static boolean generateTrackSpawner;
    public static boolean useTrackSpawnerPlaceTrack;

    // 线程配置
    public static int computeThreads;
    public static int generationThreads;
    public static int threadDutyCycle;

    // 寻路权重
    public static double elevationWeight;
    public static double heuristicWeight;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableTrackSpawner = ENABLE_TRACK_SPAWNER.get();
        generateTrackSpawner = GENERATE_TRACK_SPAWNER.get();
        useTrackSpawnerPlaceTrack = PLACE_TRACKS_USING_TRACK_SPAWNER.get();

        computeThreads = COMPUTE_THREADS.get();
        generationThreads = GENERATION_THREADS.get();
        threadDutyCycle = THREAD_DUTY_CYCLE.get();

        elevationWeight = ELEVATION_WEIGHT.get();
        heuristicWeight = HEURISTIC_WEIGHT.get();

        // 校验并修复配置值
        sanitize();

        // 运行时更新线程池
        ThreadPoolManager.resizeComputePool(computeThreads);
        ThreadPoolManager.resizeGenerationPool(generationThreads);
    }

    /**
     * 校验并修复配置值，确保在合理范围内
     */
    private static void sanitize() {
        // 线程数校验
        if (computeThreads < 0) computeThreads = 0;
        if (computeThreads > 64) computeThreads = 64;

        if (generationThreads < 1) generationThreads = 1;
        if (generationThreads > 64) generationThreads = 64;

        if (threadDutyCycle < 1) threadDutyCycle = 1;
        if (threadDutyCycle > 100) threadDutyCycle = 100;

        // 寻路权重校验
        if (elevationWeight < 0.1) elevationWeight = 0.1;
        if (elevationWeight > 10.0) elevationWeight = 10.0;

        if (heuristicWeight < 0.1) heuristicWeight = 0.1;
        if (heuristicWeight > 5.0) heuristicWeight = 5.0;
    }

    /**
     * 获取有效的计算线程数（0=自动时返回 CPU-1）
     */
    public static int getEffectiveComputeThreads() {
        if (computeThreads <= 0) {
            return Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        }
        return computeThreads;
    }
}
