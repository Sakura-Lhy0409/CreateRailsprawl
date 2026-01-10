/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.item;

import com.skua.createrailsprawl.config.RailwayConfig;
import com.skua.createrailsprawl.task.RailwayTaskManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 铁轨蓝图物品
 * 右键点击地面触发铁轨生成，支持冷却时间
 */
public class RailwayBlueprintItem extends Item {

    // 玩家冷却记录
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    public RailwayBlueprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 仅服务端执行
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.FAIL;
        }

        // 检查冷却
        UUID playerId = player.getUUID();
        long now = System.currentTimeMillis();
        int cooldownSeconds = RailwayConfig.COMMON.blueprintCooldownSeconds.get();

        Long lastUse = cooldowns.get(playerId);
        if (lastUse != null) {
            long elapsed = (now - lastUse) / 1000;
            if (elapsed < cooldownSeconds) {
                player.sendSystemMessage(Component.literal(
                        String.format("§c蓝图冷却中，还需等待 %d 秒", cooldownSeconds - elapsed)));
                return InteractionResult.FAIL;
            }
        }

        // 检查生成频率限制
        if (!RailwayTaskManager.canPlayerGenerate(playerId)) {
            player.sendSystemMessage(Component.literal("§c生成次数已达上限，请稍后再试"));
            return InteractionResult.FAIL;
        }

        ServerLevel level = (ServerLevel) context.getLevel();
        ItemStack stack = context.getItemInHand();
        int radius = RailwayConfig.COMMON.blueprintRadius.get();

        player.sendSystemMessage(Component.literal("§a铁轨蓝图激活！§r开始生成铁轨网络..."));

        // 提交异步生成任务
        String taskId = RailwayTaskManager.submitTask(player, level, context.getClickedPos(), radius);

        if (taskId != null) {
            // 记录冷却
            cooldowns.put(playerId, now);

            // 消耗物品
            if (RailwayConfig.COMMON.blueprintConsumeOnUse.get()) {
                stack.shrink(1);
            }

            return InteractionResult.CONSUME;
        } else {
            player.sendSystemMessage(Component.literal("§c生成任务提交失败"));
            return InteractionResult.FAIL;
        }
    }

    /**
     * 清除玩家冷却（用于管理命令）
     */
    public static void clearCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }

    /**
     * 清除所有冷却
     */
    public static void clearAllCooldowns() {
        cooldowns.clear();
    }
}
