package com.skua.createrailsprawl.worldgen;

import com.skua.createrailsprawl.CreateRailsprawl;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = 
            DeferredRegister.create(ForgeRegistries.FEATURES, CreateRailsprawl.MOD_ID);

    public static final RegistryObject<Feature<RailwayFeatureConfig>> RAILWAY = 
            FEATURES.register("railway", () -> new RailwayFeature(RailwayFeatureConfig.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
