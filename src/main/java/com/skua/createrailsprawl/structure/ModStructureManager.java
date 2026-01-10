/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.structure;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skua.createrailsprawl.CreateRailsprawl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;

/**
 * NBT结构模板管理器
 * 支持从资源包加载预制结构（车站、桥梁、隧道等）
 * 支持热重载
 */
@Mod.EventBusSubscriber(modid = CreateRailsprawl.MOD_ID)
public class ModStructureManager extends SimpleJsonResourceReloadListener {
    private static final String FOLDER = "railway_structure";

    // 普通车站模板
    public static final Map<Integer, StationTemplate> normalStation = new HashMap<>();
    // 地下车站模板
    public static final Map<Integer, StationTemplate> undergroundStation = new HashMap<>();

    // 路基/路面模板
    public static final Map<Integer, RailwayTemplate> ground = new HashMap<>();
    // 隧道模板
    public static final Map<Integer, RailwayTemplate> tunnel = new HashMap<>();
    // 桥梁模板
    public static final Map<Integer, RailwayTemplate> bridge = new HashMap<>();

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ModStructureManager());
    }

    public ModStructureManager() {
        super(new Gson(), FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceList, ResourceManager resourceManager, ProfilerFiller profiler) {
        // 清空现有模板
        normalStation.clear();
        undergroundStation.clear();
        ground.clear();
        tunnel.clear();
        bridge.clear();

        resourceList.forEach((location, json) -> {
            try {
                JsonObject jsonObject = json.getAsJsonObject();
                String templateClass = GsonHelper.getAsString(jsonObject, "class");
                String type = GsonHelper.getAsString(jsonObject, "type");
                String nbtPath = GsonHelper.getAsString(jsonObject, "template");
                int heightOffset = GsonHelper.getAsInt(jsonObject, "height_offset", 0);

                ResourceLocation nbtLocation = new ResourceLocation(
                        nbtPath.split(":")[0],
                        "structures/" + nbtPath.split(":")[1] + ".nbt"
                );

                CompoundTag rootTag = loadNBT(resourceManager, nbtLocation);

                if (rootTag != null) {
                    int id = location.getPath().hashCode();

                    switch (templateClass) {
                        case "station" -> {
                            switch (type) {
                                case "normal" -> normalStation.put(id,
                                        new StationTemplate(rootTag, heightOffset, id, StationTemplate.StationType.NORMAL));
                                case "underground" -> undergroundStation.put(id,
                                        new StationTemplate(rootTag, heightOffset, id, StationTemplate.StationType.UNDER_GROUND));
                            }
                        }
                        case "roadbed" -> {
                            switch (type) {
                                case "ground" -> ground.put(id, new RailwayTemplate(rootTag, heightOffset, id, type));
                                case "tunnel" -> tunnel.put(id, new RailwayTemplate(rootTag, heightOffset, id, type));
                                case "bridge" -> bridge.put(id, new RailwayTemplate(rootTag, heightOffset, id, type));
                            }
                        }
                    }

                    CreateRailsprawl.LOGGER.debug("已加载结构模板: {}", location);
                }
            } catch (Exception e) {
                CreateRailsprawl.LOGGER.error("加载结构模板失败: {}", location, e);
            }
        });

        CreateRailsprawl.LOGGER.info("已加载 {} 个车站模板, {} 个路基模板, {} 个隧道模板, {} 个桥梁模板",
                normalStation.size() + undergroundStation.size(),
                ground.size(),
                tunnel.size(),
                bridge.size());
    }

    private CompoundTag loadNBT(ResourceManager resourceManager, ResourceLocation location) {
        try {
            var resource = resourceManager.getResource(location);
            if (resource.isEmpty()) {
                CreateRailsprawl.LOGGER.warn("找不到NBT文件: {}", location);
                return null;
            }

            try (InputStream inputStream = resource.get().open();
                 DataInputStream dataStream = new DataInputStream(new BufferedInputStream(
                         new GZIPInputStream(inputStream)))) {
                return NbtIo.read(dataStream);
            }
        } catch (Exception e) {
            CreateRailsprawl.LOGGER.error("加载NBT文件失败: {}", location, e);
            return null;
        }
    }

    // --- 随机获取模板的方法 ---

    public static StationTemplate getRandomNormalStation(long seed) {
        return getRandomFromMap(normalStation, seed + 84269);
    }

    public static StationTemplate getRandomUndergroundStation(long seed) {
        return getRandomFromMap(undergroundStation, seed + 711551);
    }

    public static RailwayTemplate getRandomGround(long seed) {
        return getRandomFromMap(ground, seed + 84270);
    }

    public static RailwayTemplate getRandomTunnel(long seed) {
        return getRandomFromMap(tunnel, seed + 711553);
    }

    public static RailwayTemplate getRandomBridge(long seed) {
        return getRandomFromMap(bridge, seed + 90318);
    }

    private static <T> T getRandomFromMap(Map<Integer, T> map, long seed) {
        if (map.isEmpty()) return null;
        Random random = new Random(seed);
        var values = map.values().toArray();
        return (T) values[random.nextInt(values.length)];
    }

    // --- 检查模板是否可用 ---

    public static boolean hasStationTemplates() {
        return !normalStation.isEmpty() || !undergroundStation.isEmpty();
    }

    public static boolean hasRoadbedTemplates() {
        return !ground.isEmpty();
    }

    public static boolean hasTunnelTemplates() {
        return !tunnel.isEmpty();
    }

    public static boolean hasBridgeTemplates() {
        return !bridge.isEmpty();
    }
}
