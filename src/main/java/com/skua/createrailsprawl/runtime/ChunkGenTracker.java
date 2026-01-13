package com.skua.createrailsprawl.runtime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区块生成阶段追踪器
 * 区分世界生成阶段（WorldGenRegion）和玩家操作阶段
 */
public final class ChunkGenTracker {
    private ChunkGenTracker() {}

    private static final Set<String> GENERATING_CHUNKS = ConcurrentHashMap.newKeySet();

    public static boolean isWorldGenPhase(WorldGenLevel level) {
        return level instanceof WorldGenRegion;
    }

    @SuppressWarnings("deprecation")
    public static ServerLevel extractServerLevel(WorldGenLevel level) {
        if (level instanceof ServerLevel sl) {
            return sl;
        }
        if (level instanceof WorldGenRegion region) {
            return region.getLevel();
        }
        return null;
    }

    public static void markGenerating(ServerLevel level, int chunkX, int chunkZ) {
        GENERATING_CHUNKS.add(makeKey(level, chunkX, chunkZ));
    }

    public static void markComplete(ServerLevel level, int chunkX, int chunkZ) {
        GENERATING_CHUNKS.remove(makeKey(level, chunkX, chunkZ));
    }

    public static boolean isGenerating(ServerLevel level, int chunkX, int chunkZ) {
        return GENERATING_CHUNKS.contains(makeKey(level, chunkX, chunkZ));
    }

    public static void clearAll() {
        GENERATING_CHUNKS.clear();
    }

    private static String makeKey(ServerLevel level, int chunkX, int chunkZ) {
        return level.dimension().location() + ":" + chunkX + "," + chunkZ;
    }
}
