package com.skua.createrailsprawl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.track.TrackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @WrapOperation(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z"),
            remap = false
    )
    private boolean hasBlockEntity$patchException(BlockState instance, Operation<Boolean> original, BlockPos pos, BlockState state) {
        if (state.is(AllBlocks.TRACK.get()) && !state.getValue(TrackBlock.HAS_BE)) {
            return false;
        } else {
            return original.call(instance);
        }
    }

    @Shadow(remap = false)
    public abstract BlockState getBlockState(BlockPos pos);

    @Inject(
            method = "promotePendingBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            at = @At(value = "HEAD"), cancellable = true, remap = false
    )
    private void hasBlockEntity$patchException(BlockPos pos, CompoundTag tag, CallbackInfoReturnable<BlockEntity> cir) {
        BlockState state = this.getBlockState(pos);
        if (state.is(AllBlocks.TRACK.get()) && !state.getValue(TrackBlock.HAS_BE)) {
            cir.setReturnValue(null);
        }
    }
}
