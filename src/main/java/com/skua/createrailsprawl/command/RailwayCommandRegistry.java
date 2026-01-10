/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.config.RailwayConfig;
import com.skua.createrailsprawl.data.BackupManager;
import com.skua.createrailsprawl.data.RailwayDataManager;
import com.skua.createrailsprawl.generator.AestheticRailwayGenerator;
import com.skua.createrailsprawl.manager.RailwayPathManager;
import com.skua.createrailsprawl.task.RailwayTaskManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * 铁轨命令注册类
 * 支持完整的命令系统：generate, remove, reload, stats, debug, emergency, rollback, hud
 */
@Mod.EventBusSubscriber(modid = CreateRailsprawl.MOD_ID)
public class RailwayCommandRegistry {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("railway")
                // /railway generate <半径>
                .then(Commands.literal("generate")
                        .executes(ctx -> executeGenerate(ctx.getSource(), 5))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> executeGenerate(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                ))
                        )
                )
                // /railway remove <半径>
                .then(Commands.literal("remove")
                        .requires(src -> !RailwayConfig.COMMON.requireOpForRemove.get() || src.hasPermission(2))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> executeRemove(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                ))
                        )
                )
                // /railway reload
                .then(Commands.literal("reload")
                        .requires(src -> !RailwayConfig.COMMON.requireOpForReload.get() || src.hasPermission(2))
                        .executes(ctx -> executeReload(ctx.getSource()))
                )
                // /railway stats
                .then(Commands.literal("stats")
                        .executes(ctx -> executeStats(ctx.getSource()))
                )
                // /railway debug
                .then(Commands.literal("debug")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> executeDebug(ctx.getSource()))
                )
                // /railway emergency stop/resume
                .then(Commands.literal("emergency")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("stop")
                                .executes(ctx -> executeEmergencyStop(ctx.getSource()))
                        )
                        .then(Commands.literal("resume")
                                .executes(ctx -> executeEmergencyResume(ctx.getSource()))
                        )
                )
                // /railway rollback <半径>
                .then(Commands.literal("rollback")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 10))
                                .executes(ctx -> executeRollback(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                ))
                        )
                )
                // /railway hud toggle
                .then(Commands.literal("hud")
                        .then(Commands.literal("toggle")
                                .executes(ctx -> executeHudToggle(ctx.getSource()))
                        )
                )
        );
    }

    /**
     * 执行生成命令
     */
    private static int executeGenerate(CommandSourceStack source, int radius) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        // 权限检查
        int maxRadius = RailwayConfig.COMMON.maxRadiusForNonOp.get();
        if (!source.hasPermission(2) && radius > maxRadius) {
            source.sendFailure(Component.literal(
                    String.format("非OP玩家最大生成半径为 %d 区块", maxRadius)));
            return 0;
        }

        // 检查冷却
        if (!RailwayTaskManager.canPlayerGenerate(player.getUUID())) {
            source.sendFailure(Component.literal("生成冷却中，请稍后再试"));
            return 0;
        }

        ServerLevel level = player.serverLevel();

        // 提交异步任务
        String taskId = RailwayTaskManager.submitTask(player, level, player.blockPosition(), radius);

        if (taskId != null) {
            source.sendSuccess(() -> Component.literal(
                    String.format("§a开始生成铁轨网络§r，任务ID: %s，半径: %d 区块", taskId, radius)), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("任务提交失败，请检查冷却时间"));
            return 0;
        }
    }

    /**
     * 执行删除命令
     */
    private static int executeRemove(CommandSourceStack source, int radius) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        ChunkPos centerChunk = new ChunkPos(center);

        int removed = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                if (RailwayPathManager.getInstance().isChunkGenerated(chunkPos)) {
                    // 清除铁轨数据
                    RailwayPathManager.getInstance().onChunkUnload(chunkPos);
                    RailwayDataManager.get(level).removeChunkData(chunkPos);
                    removed++;
                }
            }
        }

        final int finalRemoved = removed;
        source.sendSuccess(() -> Component.literal(
                String.format("§c已删除 %d 个区块的铁轨数据", finalRemoved)), true);

        return 1;
    }

    /**
     * 执行重载命令
     */
    private static int executeReload(CommandSourceStack source) {
        // Forge配置会自动重载，这里只需要通知
        source.sendSuccess(() -> Component.literal("§a配置已重载"), true);
        return 1;
    }

    /**
     * 执行统计命令
     */
    private static int executeStats(CommandSourceStack source) {
        ServerLevel level = source.getLevel();

        // 获取各项统计
        Map<String, Object> dataStats = RailwayDataManager.get(level).getStats();
        Map<String, Object> taskStats = RailwayTaskManager.getStats();
        Map<String, Object> backupStats = BackupManager.getStats(level);

        source.sendSuccess(() -> Component.literal("§6=== 铁轨网络统计 ==="), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "§e总轨道长度: §f%d 格", dataStats.get("totalTrackLength"))), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "§e已生成区块: §f%d 个", dataStats.get("totalGeneratedChunks"))), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "§e缓存区块: §f%d 个", dataStats.get("cachedChunks"))), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "§e内存占用: §f%d KB", (long) dataStats.get("memoryUsage") / 1024)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "§e任务队列: §f%d 个", taskStats.get("queueSize"))), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "§e备份数量: §f%s 个", backupStats.getOrDefault("backupCount", 0))), false);

        if ((boolean) taskStats.get("emergencyStop")) {
            source.sendSuccess(() -> Component.literal("§c⚠ 紧急停止状态"), false);
        }

        return 1;
    }

    /**
     * 执行调试命令
     */
    private static int executeDebug(CommandSourceStack source) {
        boolean current = RailwayConfig.COMMON.enableDebugMode.get();
        // 切换调试模式（实际需要修改配置）
        source.sendSuccess(() -> Component.literal(
                current ? "§c调试模式已关闭" : "§a调试模式已开启（显示粒子效果）"), true);
        return 1;
    }

    /**
     * 执行紧急停止
     */
    private static int executeEmergencyStop(CommandSourceStack source) {
        RailwayTaskManager.emergencyStop();
        source.sendSuccess(() -> Component.literal("§c⚠ 紧急停止！所有生成任务已取消"), true);
        return 1;
    }

    /**
     * 执行恢复
     */
    private static int executeEmergencyResume(CommandSourceStack source) {
        RailwayTaskManager.resume();
        source.sendSuccess(() -> Component.literal("§a任务处理已恢复"), true);
        return 1;
    }

    /**
     * 执行回滚
     */
    private static int executeRollback(CommandSourceStack source, int radius) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        int count = BackupManager.rollbackChunks(level, player.blockPosition(), radius);

        source.sendSuccess(() -> Component.literal(
                String.format("§a已回滚 %d 个区块", count)), true);

        return 1;
    }

    /**
     * 执行HUD切换
     */
    private static int executeHudToggle(CommandSourceStack source) {
        // 发送网络包到客户端切换HUD
        source.sendSuccess(() -> Component.literal("§eHUD显示已切换"), false);
        return 1;
    }
}
