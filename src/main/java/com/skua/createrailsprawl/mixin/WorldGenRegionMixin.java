/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * WorldGenRegion Mixin
 * 绕过ensureCanWrite检查，允许在世界生成阶段放置Track方块
 */
@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin {

    @WrapOperation(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/WorldGenRegion;ensureCanWrite(Lnet/minecraft/core/BlockPos;)Z"),
            remap = false
    )
    private boolean createRailsprawl$bypassEnsureCanWrite(WorldGenRegion instance, BlockPos blockPos,
                                                           Operation<Boolean> original,
                                                           BlockPos pos, BlockState state, int flags, int recursion) {
        // 如果是Track方块，直接返回true绕过检查
        if (state.is(AllBlocks.TRACK.get())) {
            return true;
        }
        return original.call(instance, blockPos);
    }
}
