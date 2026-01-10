/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.mixin;

import com.skua.createrailsprawl.planner.RoutePlanner.RegionPos;
import com.skua.createrailsprawl.util.MyMth;
import com.skua.createrailsprawl.worldgen.RailwayBuilder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 世界生成入口Mixin
 * 在buildSurface阶段触发铁路生成
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {

    @Inject(method = "buildSurface", at = @At("HEAD"), remap = false)
    public void createRailsprawl$onBuildSurface(WorldGenRegion level, StructureManager structureManager,
                                                 RandomState random, ChunkAccess chunk, CallbackInfo ci) {
        var dimensionType = level.dimensionType();
        // 只在主世界生成铁路
        if (dimensionType.effectsLocation().toString().equals("minecraft:overworld")) {
            RegionPos regionPos = MyMth.regionPosFromChunkPos(chunk.getPos());
            RailwayBuilder builder = RailwayBuilder.getInstance(level.getSeed(), level);
            if (builder != null) {
                builder.generateRailway(regionPos);
            }
        }
    }
}
