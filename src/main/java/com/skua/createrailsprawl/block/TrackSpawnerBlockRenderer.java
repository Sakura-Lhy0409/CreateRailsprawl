package com.skua.createrailsprawl.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class TrackSpawnerBlockRenderer implements BlockEntityRenderer<TrackSpawnerBlockEntity> {

    public TrackSpawnerBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TrackSpawnerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    }
}
