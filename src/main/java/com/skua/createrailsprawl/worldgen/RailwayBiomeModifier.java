/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skua.createrailsprawl.CreateRailsprawl;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 铁路生物群系修改器
 * 将铁路Feature添加到所有主世界生物群系
 */
public record RailwayBiomeModifier(HolderSet<Biome> biomes, Holder<PlacedFeature> feature) implements BiomeModifier {

    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, CreateRailsprawl.MOD_ID);

    public static final RegistryObject<Codec<RailwayBiomeModifier>> RAILWAY_BIOME_MODIFIER =
            BIOME_MODIFIER_SERIALIZERS.register("railway", () ->
                    RecordCodecBuilder.create(builder -> builder.group(
                            Biome.LIST_CODEC.fieldOf("biomes").forGetter(RailwayBiomeModifier::biomes),
                            PlacedFeature.CODEC.fieldOf("feature").forGetter(RailwayBiomeModifier::feature)
                    ).apply(builder, RailwayBiomeModifier::new)));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && biomes.contains(biome)) {
            builder.getGenerationSettings().addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, feature);
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return RAILWAY_BIOME_MODIFIER.get();
    }
}
