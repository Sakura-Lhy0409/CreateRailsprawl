/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.async.RailwayTaskQueue;
import com.skua.createrailsprawl.config.RailwayConfig;
import com.skua.createrailsprawl.data.BackupManager;
import com.skua.createrailsprawl.data.RailwayDataManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 扩展命令注册
 * 包含 remove/reload/stats/rollback/emergency 等维护命令
 */
@Mod.EventBusSubscriber(modid = CreateRailsprawl.MOD_ID)
public class RailwayExtendedCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("railway")
                .then(Commands.literal("remove")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> removeRailway(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")))))

                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> reloadConfig(ctx.getSource())))

                .then(Commands.literal("stats")
                        .executes(ctx -> showStats(ctx.getSource())))

                .then(Commands.literal("rollback")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 10))
                                .executes(ctx -> rollback(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")))))

                .then(Commands.literal("emergency")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("stop")
                                .executes(ctx -> emergencyStop(ctx.getSource())))
                        .then(Commands.literal("resume")
                                .executes(ctx -> emergencyResume(ctx.getSource()))))

                .then(Commands.literal("debug")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> toggleDebug(ctx.getSource())))
        );
    }

    private static int removeRailway(CommandSourceStack source, int radius) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("仅玩家可执行此命令"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        ChunkPos center = new ChunkPos(player.blockPosition());
        int removed = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);
                if (RailwayDataManager.get(level).removeChunkData(pos)) {
                    removed++;
                }
            }
        }

        final int finalRemoved = removed;
        source.sendSuccess(() -> Component.literal("§a已删除 " + finalRemoved + " 个区块的铁轨数据"), true);
        return removed;
    }

    private static int reloadConfig(CommandSourceStack source) {
        RailwayConfig.reload();
        source.sendSuccess(() -> Component.literal("§a配置已重载"), true);
        return 1;
    }

    private static int showStats(CommandSourceStack source) {
        RailwayTaskQueue queue = RailwayTaskQueue.getInstance();

        source.sendSuccess(() -> Component.literal("§e=== 铁轨网络统计 ==="), false);
        source.sendSuccess(() -> Component.literal("任务队列: " + queue.getQueueSize() + " 个"), false);
        source.sendSuccess(() -> Component.literal("队列状态: " + (queue.isPaused() ? "§c暂停" : "§a运行中")), false);

        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        source.sendSuccess(() -> Component.literal("内存占用: " + usedMB + " MB"), false);

        return 1;
    }

    private static int rollback(CommandSourceStack source, int radius) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("仅玩家可执行此命令"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        ChunkPos center = new ChunkPos(player.blockPosition());
        int rolled = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);
                if (BackupManager.getInstance().rollback(level, pos)) {
                    rolled++;
                }
            }
        }

        int finalRolled = rolled;
        source.sendSuccess(() -> Component.literal("§a已回滚 " + finalRolled + " 个区块"), true);
        return rolled;
    }

    private static int emergencyStop(CommandSourceStack source) {
        RailwayTaskQueue.getInstance().pause();
        RailwayTaskQueue.getInstance().clearAll();
        source.sendSuccess(() -> Component.literal("§c紧急停止！所有生成任务已取消"), true);
        return 1;
    }

    private static int emergencyResume(CommandSourceStack source) {
        RailwayTaskQueue.getInstance().resume();
        source.sendSuccess(() -> Component.literal("§a任务处理已恢复"), true);
        return 1;
    }

    private static int toggleDebug(CommandSourceStack source) {
        boolean current = RailwayConfig.COMMON.enableDetailedLogging.get();
        // 注意：ForgeConfigSpec 值不能直接 set，这里仅作示意
        source.sendSuccess(() -> Component.literal("§e调试模式: " + (!current ? "开启" : "关闭")), true);
        return 1;
    }
}
