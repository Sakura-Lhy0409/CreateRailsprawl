/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlockEntityTilt;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * TrackBlockEntity Mixin
 * 注入预生成连接功能，用于世界生成阶段添加BezierConnection
 */
@Mixin(value = TrackBlockEntity.class, remap = false)
public abstract class TrackBlockEntityMixin extends SmartBlockEntity implements ITrackPreGenExtension {

    @Shadow
    public abstract void addConnection(BezierConnection connection);

    @Shadow
    public TrackBlockEntityTilt tilt;

    @Unique
    List<BezierConnection> createRailsprawl$preConnections = new ArrayList<>();

    public TrackBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Unique
    @Override
    public void addConnectionToPreGen(BezierConnection connection) {
        createRailsprawl$preConnections.add(connection);
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private void createRailsprawl$onTick(CallbackInfo ci) {
        if (!createRailsprawl$preConnections.isEmpty() && !level.isClientSide()) {
            createRailsprawl$preConnections.forEach(this::addConnection);
            tilt.tryApplySmoothing();
            createRailsprawl$preConnections.clear();
        }
    }
}
