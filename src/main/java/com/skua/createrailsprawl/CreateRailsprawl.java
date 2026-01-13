package com.skua.createrailsprawl;

import com.skua.createrailsprawl.block.ModBlockEntities;
import com.skua.createrailsprawl.block.ModBlocks;
import com.skua.createrailsprawl.block.TrackSpawnerBlockRenderer;
import com.skua.createrailsprawl.command.RailsprawlCommand;
import com.skua.createrailsprawl.runtime.CacheManager;
import com.skua.createrailsprawl.runtime.ThreadPoolManager;
import com.skua.createrailsprawl.worldgen.ModFeatures;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CreateRailsprawl.MOD_ID)
public class CreateRailsprawl {
    public static final String MOD_ID = "createrailsprawl";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final int CHUNK_GROUP_SIZE = 128;
    public static final int HEIGHT_MAX_INCREMENT = 60;

    public CreateRailsprawl() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModFeatures.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RailwayConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ThreadPoolManager.onServerStarted(event.getServer());
        CacheManager.onServerStarted();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ThreadPoolManager.onServerStopping();
        CacheManager.onServerStopping();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RailsprawlCommand.register(event.getDispatcher());
        LOGGER.info("Railsprawl commands registered");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                BlockEntityRenderers.register(ModBlockEntities.TRACK_SPAWNER.get(), TrackSpawnerBlockRenderer::new);
            });
        }
    }
}
