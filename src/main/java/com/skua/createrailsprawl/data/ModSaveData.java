/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.data;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.planner.RoutePlanner.RegionPos;
import com.skua.createrailsprawl.worldgen.RailwayMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 铁路数据持久化
 * 使用Minecraft原生存档系统按区域存储RailwayMap
 */
public class ModSaveData extends SavedData {
    private static final String DATA_NAME = CreateRailsprawl.MOD_ID + "_railway_data";

    // 区域铁路数据
    private final Map<RegionPos, RailwayMap> regionRailways = new ConcurrentHashMap<>();

    public ModSaveData() {
    }

    public ModSaveData(CompoundTag tag) {
        load(tag);
    }

    /**
     * 获取或创建SavedData实例
     */
    public static ModSaveData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                ModSaveData::new,
                ModSaveData::new,
                DATA_NAME
        );
    }

    /**
     * 加载数据
     */
    private void load(CompoundTag tag) {
        if (tag.contains("regions")) {
            ListTag regionsTag = tag.getList("regions", 10);
            for (int i = 0; i < regionsTag.size(); i++) {
                try {
                    CompoundTag regionTag = regionsTag.getCompound(i);
                    RailwayMap map = RailwayMap.fromNBT(regionTag);
                    regionRailways.put(map.regionPos, map);
                } catch (Exception e) {
                    CreateRailsprawl.LOGGER.error("加载区域铁路数据失败", e);
                }
            }
        }
        CreateRailsprawl.LOGGER.info("已加载 {} 个区域的铁路数据", regionRailways.size());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag regionsTag = new ListTag();
        for (RailwayMap map : regionRailways.values()) {
            try {
                regionsTag.add(map.toNBT());
            } catch (Exception e) {
                CreateRailsprawl.LOGGER.error("保存区域 {} 铁路数据失败", map.regionPos, e);
            }
        }
        tag.put("regions", regionsTag);
        CreateRailsprawl.LOGGER.debug("已保存 {} 个区域的铁路数据", regionRailways.size());
        return tag;
    }

    /**
     * 获取区域铁路数据
     */
    public RailwayMap getRailwayMap(RegionPos regionPos) {
        return regionRailways.get(regionPos);
    }

    /**
     * 设置区域铁路数据
     */
    public void setRailwayMap(RegionPos regionPos, RailwayMap map) {
        regionRailways.put(regionPos, map);
        setDirty();
    }

    /**
     * 检查区域是否已生成
     */
    public boolean hasRegion(RegionPos regionPos) {
        RailwayMap map = regionRailways.get(regionPos);
        return map != null && map.isPlanned();
    }

    /**
     * 获取所有区域数据
     */
    public Map<RegionPos, RailwayMap> getAllRegions() {
        return regionRailways;
    }

    /**
     * 清除所有数据
     */
    public void clear() {
        regionRailways.clear();
        setDirty();
    }
}
