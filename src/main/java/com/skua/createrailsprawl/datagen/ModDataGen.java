package com.skua.createrailsprawl.datagen;

import com.skua.createrailsprawl.CreateRailsprawl;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateRailsprawl.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDataGen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        // 语言文件
        generator.addProvider(event.includeClient(), new ModLanguageProviderEN(output));
        generator.addProvider(event.includeClient(), new ModLanguageProviderZH(output));

        // 方块状态和模型
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(output, existingFileHelper));

        // 物品模型
        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, existingFileHelper));

        // 战利品表
        generator.addProvider(event.includeServer(), new ModLootTableProvider(output));
    }
}
