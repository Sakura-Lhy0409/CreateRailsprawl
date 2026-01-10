package com.skua.createrailsprawl.worldgen;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.railway.RailwayBuilder;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class RailwayFeature extends Feature<RailwayFeatureConfig> {
    public RailwayFeature(com.mojang.serialization.Codec<RailwayFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RailwayFeatureConfig> context) {
        // Railway generation is handled by NoiseBasedChunkGeneratorMixin
        return true;
    }
}
