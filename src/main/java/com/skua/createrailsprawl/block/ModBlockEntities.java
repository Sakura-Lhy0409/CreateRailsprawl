/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.block;

import com.skua.createrailsprawl.CreateRailsprawl;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CreateRailsprawl.MOD_ID);

    public static final RegistryObject<BlockEntityType<TrackSpawnerBlockEntity>> TRACK_SPAWNER =
            BLOCK_ENTITIES.register("track_spawner",
                    () -> BlockEntityType.Builder.of(TrackSpawnerBlockEntity::new, ModBlocks.TRACK_SPAWNER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
