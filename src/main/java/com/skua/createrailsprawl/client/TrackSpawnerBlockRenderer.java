package com.skua.createrailsprawl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.skua.createrailsprawl.block.TrackSpawnerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * TrackSpawner 方块渲染器
 * 在方块上显示旋转的铁轨物品
 */
public class TrackSpawnerBlockRenderer implements BlockEntityRenderer<TrackSpawnerBlockEntity> {
    private static final ItemStack TRACK_ITEM = new ItemStack(AllBlocks.TRACK.get());

    public TrackSpawnerBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TrackSpawnerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // 旋转动画
        float time = (blockEntity.getLevel().getGameTime() + partialTick) * 2.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(time % 360));
        poseStack.scale(0.5f, 0.5f, 0.5f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                TRACK_ITEM, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                poseStack, buffer, blockEntity.getLevel(), 0);

        poseStack.popPose();
    }
}
