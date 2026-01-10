/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl;

import com.skua.createrailsprawl.async.RailwayTaskQueue;
import com.skua.createrailsprawl.client.RailwayHudRenderer;
import com.skua.createrailsprawl.client.RailwayKeyBindings;
import com.skua.createrailsprawl.command.RailwayCommandRegistry;
import com.skua.createrailsprawl.command.RailwayExtendedCommands;
import com.skua.createrailsprawl.config.RailwayConfig;
import com.skua.createrailsprawl.data.BackupManager;
import com.skua.createrailsprawl.data.RailwayDataManager;
import com.skua.createrailsprawl.event.RailwayChunkEventListener;
import com.skua.createrailsprawl.integration.ClaimIntegration;
import com.skua.createrailsprawl.manager.RailwayPathManager;
import com.skua.createrailsprawl.network.NetworkHandler;
import com.skua.createrailsprawl.registry.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Create Railsprawl (机械动力·铁轨蔓延) 主类
 * 生成机械动力风格的铁轨群系，支持主动生成与被动跑图生成双模式
 */
@Mod(CreateRailsprawl.MOD_ID)
public class CreateRailsprawl {
    public static final String MOD_ID = "createrailsprawl";
    public static final Logger LOGGER = LogManager.getLogger();

    public CreateRailsprawl() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置
        RailwayConfig.register();

        // 注册物品
        ModItems.register(modEventBus);

        // 注册生命周期事件
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        // 客户端 MOD 事件（快捷键注册）
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener(RailwayKeyBindings::onRegisterKeyMappings);
        });

        // 注册 Forge 事件（仅注册主类实例，其他类使用 @Mod.EventBusSubscriber 自动注册）
        MinecraftForge.EVENT_BUS.register(this);

        // 客户端 Forge 事件（RailwayHudRenderer 已有注解自动注册，RailwayKeyBindings 需要手动注册按键事件）
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            MinecraftForge.EVENT_BUS.register(RailwayKeyBindings.class);
        });

        LOGGER.info("[RailwayMod] Create Railsprawl 初始化完成");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
            ClaimIntegration.init();
            LOGGER.info("[RailwayMod] 通用设置完成");
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            RailwayKeyBindings.register();
            LOGGER.info("[RailwayMod] 客户端设置完成");
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        RailwayTaskQueue.getInstance().start();
        LOGGER.info("[RailwayMod] 服务器启动，任务队列已启动");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        RailwayTaskQueue.getInstance().shutdown();
        BackupManager.getInstance().shutdown();
        RailwayPathManager.getInstance().clear();
        RailwayDataManager.reset();
        LOGGER.info("[RailwayMod] 服务器关闭，所有系统已清理");
    }
}
