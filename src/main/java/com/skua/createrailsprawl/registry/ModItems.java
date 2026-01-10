/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.registry;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.item.RailwayBlueprintItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册类
 * 使用 Forge DeferredRegister 规范注册所有物品
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CreateRailsprawl.MOD_ID);

    // 铁轨蓝图物品 - 可堆叠，右键触发半径3区块的铁轨生成
    public static final RegistryObject<Item> RAILWAY_BLUEPRINT = ITEMS.register("railway_blueprint",
            () -> new RailwayBlueprintItem(new Item.Properties().stacksTo(64)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        eventBus.addListener(ModItems::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(RAILWAY_BLUEPRINT);
        }
    }
}
