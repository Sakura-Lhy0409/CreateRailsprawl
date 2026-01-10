package com.skua.createrailsprawl.datagen;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CreateRailsprawl.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // 铁轨蓝图 - 简单2D物品
        basicItem(ModItems.RAILWAY_BLUEPRINT.get());
    }
}
