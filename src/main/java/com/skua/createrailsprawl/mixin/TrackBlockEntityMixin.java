/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
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
@Mixin(TrackBlockEntity.class)
public abstract class TrackBlockEntityMixin extends SmartBlockEntity implements ITrackPreGenExtension {

    @Shadow
    public abstract void addConnection(BezierConnection connection);

    // 预生成连接列表
    @Unique
    private final List<BezierConnection> createRailsprawl$preConnections = new ArrayList<>();

    public TrackBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addConnectionToPreGen(BezierConnection connection) {
        createRailsprawl$preConnections.add(connection);
    }

    /**
     * 在tick时添加预生成的连接
     */
    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private void createRailsprawl$onTick(CallbackInfo ci) {
        if (!createRailsprawl$preConnections.isEmpty() && level != null && !level.isClientSide()) {
            for (BezierConnection connection : createRailsprawl$preConnections) {
                try {
                    this.addConnection(connection);
                } catch (Exception e) {
                    // 忽略添加失败的连接
                }
            }
            createRailsprawl$preConnections.clear();
        }
    }
}
