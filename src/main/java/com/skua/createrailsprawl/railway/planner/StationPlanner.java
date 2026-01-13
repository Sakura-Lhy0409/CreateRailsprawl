package com.skua.createrailsprawl.railway.planner;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.railway.RegionPos;
import com.skua.createrailsprawl.structure.ModStructureManager;
import com.skua.createrailsprawl.structure.StationTemplate;
import com.skua.createrailsprawl.util.MyMth;
import com.skua.createrailsprawl.util.MyRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static com.skua.createrailsprawl.CreateRailsprawl.CHUNK_GROUP_SIZE;
import static com.skua.createrailsprawl.CreateRailsprawl.HEIGHT_MAX_INCREMENT;

public class StationPlanner {
    private final RegionPos regionPos;

    public StationPlanner(RegionPos regionPos) {
        this.regionPos = regionPos;
    }

    public static List<StationGenInfo> generateStation(RegionPos regionPos, ServerLevel level, long seed) {
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState cfg = level.getChunkSource().randomState();
        long regionSeed = seed + regionPos.hashCode();
        List<StationGenInfo> result = new ArrayList<>();
        int[] pos = MyRandom.generatePoints(regionSeed, CHUNK_GROUP_SIZE);
        ChunkPos chunkPos = new ChunkPos(MyMth.chunkPosXFromRegionPos(regionPos, pos[0]), MyMth.chunkPosZFromRegionPos(regionPos, pos[1]));

        int x = chunkPos.getBlockX(0), z = chunkPos.getBlockZ(0);
        int y = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE, level, cfg);
        int h = y;

        int miny = 2550;
        for (int ix = -2; ix < 3; ix++) {
            for (int iz = -2; iz < 3; iz++) {
                int ty = gen.getBaseHeight(ix * 32 + x, iz * 32 + z, Heightmap.Types.WORLD_SURFACE, level, cfg);
                miny = Math.min(miny, ty);
            }
        }
        if (y - miny > 20) h = miny;
        h = Math.max(h, level.getSeaLevel());
        h = Math.min(h, level.getSeaLevel() + HEIGHT_MAX_INCREMENT);

        StationTemplate station;
        if (h < y - 10) station = ModStructureManager.getRandomUnderGroundStation(regionSeed);
        else station = ModStructureManager.getRandomNormalStation(regionSeed);
        result.add(new StationGenInfo(station, new BlockPos(x, h, z)));
        return result;
    }

    public List<ConnectionGenInfo> generateConnections(ServerLevel level, long seed) {
        List<ConnectionGenInfo> result = new ArrayList<>();
        List<StationGenInfo> thisStations = generateStation(regionPos, level, seed);
        List<StationGenInfo> north = generateStation(new RegionPos(regionPos.x(), regionPos.z()-1), level, seed);
        List<StationGenInfo> south = generateStation(new RegionPos(regionPos.x(), regionPos.z()+1), level, seed);
        List<StationGenInfo> east = generateStation(new RegionPos(regionPos.x()+1, regionPos.z()), level, seed);
        List<StationGenInfo> west = generateStation(new RegionPos(regionPos.x()-1, regionPos.z()), level, seed);

        var thisAssignedExits = assignExits(getExitsPos(thisStations));
        var northAssignedExits = assignExits(getExitsPos(north));
        var southAssignedExits = assignExits(getExitsPos(south));
        var eastAssignedExits = assignExits(getExitsPos(east));
        var westAssignedExits = assignExits(getExitsPos(west));

        var tpos = thisStations.get(0).placePos;
        CreateRailsprawl.LOGGER.info("====> StationPlanner: {} {} {} {}", tpos.getX(), tpos.getY(), tpos.getZ(), regionPos);

        result.add(ConnectionGenInfo.getConnectionInfo(thisAssignedExits.get(3), eastAssignedExits.get(2), new Vec3(1, 0, 0)));
        result.add(ConnectionGenInfo.getConnectionInfo(westAssignedExits.get(3), thisAssignedExits.get(2), new Vec3(1, 0, 0)));
        result.add(ConnectionGenInfo.getConnectionInfo(northAssignedExits.get(1), thisAssignedExits.get(0), new Vec3(0, 0, 1)));
        result.add(ConnectionGenInfo.getConnectionInfo(thisAssignedExits.get(1), southAssignedExits.get(0), new Vec3(0, 0, 1)));
        return result;
    }

    private List<StationTemplate.Exit> getExitsPos(List<StationGenInfo> stations) {
        List<StationTemplate.Exit> exits = new ArrayList<>();
        for (StationGenInfo station : stations) {
            BlockPos placePos = station.placePos;
            for (StationTemplate.Exit exit : station.stationTemplate.getExits()) {
                exits.add(new StationTemplate.Exit(placePos.offset(exit.exitPos()), exit.dir()));
            }
        }
        return exits;
    }

    private List<StationTemplate.Exit> assignExits(List<StationTemplate.Exit> exits) {
        if (exits.size() < 4) return exits;
        List<StationTemplate.Exit> copy = new ArrayList<>(exits);
        List<StationTemplate.Exit> result = new ArrayList<>();
        copy.sort(Comparator.comparingDouble(e -> {
            int z = e.exitPos().getZ();
            return z + new Random(75_1049 + z).nextDouble() * 2 - 1;
        }));
        result.add(copy.remove(0));
        result.add(copy.remove(copy.size()-1));
        copy.sort(Comparator.comparingDouble(e -> {
            int x = e.exitPos().getX();
            return x + new Random(75_1052 + x).nextDouble() * 2 - 1;
        }));
        result.add(copy.remove(0));
        result.add(copy.remove(copy.size()-1));
        Set<StationTemplate.Exit> addedExits = new HashSet<>(result);
        for (StationTemplate.Exit exit : exits) {
            if (!addedExits.contains(exit)) result.add(exit);
        }
        return result;
    }

    private static int[] getConnectStart(BlockPos exitPos, Vec3 dir, Vec3 offset, Vec3 exitDir) {
        Vec3 pos = Vec3.atCenterOf(exitPos);
        Vec3 addOff = exitDir.scale(40);
        Vec3 start = pos.add(dir.scale(30).add(addOff));
        return new int[] {(int) start.x, (int) start.z, exitPos.getY()};
    }

    public record StationGenInfo(StationTemplate stationTemplate, BlockPos placePos) {
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("id", stationTemplate.getId());
            tag.putString("type", stationTemplate.getType().name());
            tag.putInt("x", placePos.getX());
            tag.putInt("y", placePos.getY());
            tag.putInt("z", placePos.getZ());
            return tag;
        }

        public static StationGenInfo fromNBT(CompoundTag tag) {
            int id = tag.getInt("id"), x = tag.getInt("x"), y = tag.getInt("y"), z = tag.getInt("z");
            StationTemplate.StationType type = StationTemplate.StationType.valueOf(tag.getString("type"));
            StationTemplate stationTemplate = null;
            switch (type) {
                case NORMAL -> { if (ModStructureManager.normalStation.containsKey(id)) stationTemplate = ModStructureManager.normalStation.get(id); }
                case UNDER_GROUND -> { if (ModStructureManager.undergroundStation.containsKey(id)) stationTemplate = ModStructureManager.undergroundStation.get(id); }
            }
            return new StationGenInfo(stationTemplate, new BlockPos(x, y, z));
        }
    }

    public record ConnectionGenInfo(Vec3 start, Vec3 startDir, Vec3 end, Vec3 endDir, int[] connectStart, int[] connectEnd, Vec3 exitDir) {
        public static ConnectionGenInfo getConnectionInfo(StationTemplate.Exit A, StationTemplate.Exit B, Vec3 exitDir) {
            Vec3 APos = new Vec3(A.exitPos().getX(), A.exitPos().getY(), A.exitPos().getZ());
            Vec3 BPos = new Vec3(B.exitPos().getX(), B.exitPos().getY(), B.exitPos().getZ());
            return new ConnectionGenInfo(APos, A.dir(), BPos, B.dir(),
                    getConnectStart(A.exitPos(), A.dir(), BPos.subtract(APos), exitDir),
                    getConnectStart(B.exitPos(), B.dir(), APos.subtract(BPos), exitDir.reverse()),
                    exitDir);
        }
    }
}
