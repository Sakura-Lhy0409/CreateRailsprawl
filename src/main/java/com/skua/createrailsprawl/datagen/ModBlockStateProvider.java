package com.skua.createrailsprawl.datagen;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, CreateRailsprawl.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // TrackSpawner 方块 - 顶部/底部/侧面不同纹理
        simpleBlockWithItem(ModBlocks.TRACK_SPAWNER.get(),
                models().cubeBottomTop("track_spawner",
                        new ResourceLocation(CreateRailsprawl.MOD_ID, "block/track_spawner"),
                        new ResourceLocation(CreateRailsprawl.MOD_ID, "block/track_spawner_bottom"),
                        new ResourceLocation(CreateRailsprawl.MOD_ID, "block/track_spawner_top")));
    }
}
