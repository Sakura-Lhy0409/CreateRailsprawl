package com.skua.createrailsprawl.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class RailwayFeatureConfig implements FeatureConfiguration {
    public static final Codec<RailwayFeatureConfig> CODEC = Codec.unit(RailwayFeatureConfig::new);
    
    public RailwayFeatureConfig() {
    }
}
