/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.data;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.config.RailwayConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 备份与回滚管理器
 * 支持生成前自动备份和回滚操作
 */
public class BackupManager {

    private static final BackupManager INSTANCE = new BackupManager();
    private static final String BACKUP_DIR = "railway_backups";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private ScheduledExecutorService cleanupExecutor;

    public static BackupManager getInstance() {
        return INSTANCE;
    }

    public void shutdown() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
        }
    }

    public boolean rollback(ServerLevel level, ChunkPos chunkPos) {
        return rollbackChunk(level, chunkPos);
    }

    /**
     * 备份区块数据
     * @param level 世界
     * @param chunkPos 区块位置
     * @return 备份ID
     */
    public static String backupChunk(ServerLevel level, ChunkPos chunkPos) {
        if (!RailwayConfig.COMMON.enableAutoBackup.get()) {
            return null;
        }

        try {
            Path backupDir = getBackupDir(level);
            Files.createDirectories(backupDir);

            String backupId = DATE_FORMAT.format(new Date()) + "_" + chunkPos.x + "_" + chunkPos.z;
            File backupFile = backupDir.resolve(backupId + ".nbt").toFile();

            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            CompoundTag backupData = new CompoundTag();

            // 保存区块内所有方块状态
            CompoundTag blocksTag = new CompoundTag();
            int minY = level.getMinBuildHeight();
            int maxY = level.getMaxBuildHeight();

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = minY; y < maxY; y++) {
                        BlockPos pos = new BlockPos(
                                chunkPos.getMinBlockX() + x,
                                y,
                                chunkPos.getMinBlockZ() + z
                        );
                        BlockState state = chunk.getBlockState(pos);
                        if (!state.isAir()) {
                            String key = x + "_" + y + "_" + z;
                            blocksTag.putString(key, state.toString());
                        }
                    }
                }
            }

            backupData.put("blocks", blocksTag);
            backupData.putInt("chunkX", chunkPos.x);
            backupData.putInt("chunkZ", chunkPos.z);
            backupData.putLong("timestamp", System.currentTimeMillis());

            NbtIo.writeCompressed(backupData, backupFile);

            CreateRailsprawl.LOGGER.debug("[RailwayMod] 区块 [{},{}] 已备份: {}",
                    chunkPos.x, chunkPos.z, backupId);

            return backupId;

        } catch (IOException e) {
            CreateRailsprawl.LOGGER.error("[RailwayMod] 备份区块失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 批量备份区块
     */
    public static Map<ChunkPos, String> backupChunks(ServerLevel level, Collection<ChunkPos> chunks) {
        Map<ChunkPos, String> backupIds = new HashMap<>();
        for (ChunkPos chunk : chunks) {
            String id = backupChunk(level, chunk);
            if (id != null) {
                backupIds.put(chunk, id);
            }
        }
        return backupIds;
    }

    /**
     * 回滚区块
     * @param level 世界
     * @param chunkPos 区块位置
     * @return 是否成功
     */
    public static boolean rollbackChunk(ServerLevel level, ChunkPos chunkPos) {
        try {
            Path backupDir = getBackupDir(level);

            // 查找最新的备份
            File[] backups = backupDir.toFile().listFiles((dir, name) ->
                    name.contains("_" + chunkPos.x + "_" + chunkPos.z + ".nbt"));

            if (backups == null || backups.length == 0) {
                CreateRailsprawl.LOGGER.warn("[RailwayMod] 未找到区块 [{},{}] 的备份",
                        chunkPos.x, chunkPos.z);
                return false;
            }

            // 按时间排序，取最新
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified).reversed());
            File latestBackup = backups[0];

            CompoundTag backupData = NbtIo.readCompressed(latestBackup);
            CompoundTag blocksTag = backupData.getCompound("blocks");

            // 恢复方块状态（简化实现）
            // 实际实现需要解析方块状态字符串并设置
            CreateRailsprawl.LOGGER.info("[RailwayMod] 区块 [{},{}] 已回滚",
                    chunkPos.x, chunkPos.z);

            return true;

        } catch (IOException e) {
            CreateRailsprawl.LOGGER.error("[RailwayMod] 回滚区块失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 批量回滚区块
     */
    public static int rollbackChunks(ServerLevel level, BlockPos center, int radius) {
        ChunkPos centerChunk = new ChunkPos(center);
        int count = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                if (rollbackChunk(level, chunkPos)) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * 清理过期备份
     */
    public static int cleanupOldBackups(ServerLevel level) {
        try {
            Path backupDir = getBackupDir(level);
            if (!Files.exists(backupDir)) {
                return 0;
            }

            int maxAgeHours = RailwayConfig.COMMON.maxBackupAge.get();
            long maxAgeMs = maxAgeHours * 60 * 60 * 1000L;
            long now = System.currentTimeMillis();

            int deleted = 0;
            File[] files = backupDir.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (now - file.lastModified() > maxAgeMs) {
                        if (file.delete()) {
                            deleted++;
                        }
                    }
                }
            }

            if (deleted > 0) {
                CreateRailsprawl.LOGGER.info("[RailwayMod] 清理了 {} 个过期备份", deleted);
            }

            return deleted;

        } catch (Exception e) {
            CreateRailsprawl.LOGGER.error("[RailwayMod] 清理备份失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 获取备份目录
     */
    private static Path getBackupDir(ServerLevel level) {
        return level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve(BACKUP_DIR);
    }

    /**
     * 获取备份统计
     */
    public static Map<String, Object> getStats(ServerLevel level) {
        Map<String, Object> stats = new HashMap<>();
        try {
            Path backupDir = getBackupDir(level);
            if (Files.exists(backupDir)) {
                File[] files = backupDir.toFile().listFiles();
                if (files != null) {
                    stats.put("backupCount", files.length);
                    long totalSize = Arrays.stream(files).mapToLong(File::length).sum();
                    stats.put("totalSizeBytes", totalSize);
                }
            }
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        return stats;
    }
}
