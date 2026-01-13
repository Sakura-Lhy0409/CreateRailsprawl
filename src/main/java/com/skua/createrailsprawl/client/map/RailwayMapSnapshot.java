package com.skua.createrailsprawl.client.map;

import com.skua.createrailsprawl.railway.planner.StationPlanner;
import com.skua.createrailsprawl.util.CurveRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;

/**
 * 铁路网地图快照 - 用于客户端渲染
 */
public class RailwayMapSnapshot {
    private final List<StationInfo> stations;
    private final List<ConnectionInfo> connections;
    private final List<List<BlockPos>> railPolylines;
    private final int minX, minZ, maxX, maxZ;

    public RailwayMapSnapshot(List<StationInfo> stations, List<ConnectionInfo> connections, List<List<BlockPos>> railPolylines) {
        this.stations = Collections.unmodifiableList(new ArrayList<>(stations != null ? stations : List.of()));
        this.connections = Collections.unmodifiableList(new ArrayList<>(connections != null ? connections : List.of()));
        List<List<BlockPos>> rp = new ArrayList<>();
        if (railPolylines != null) {
            for (List<BlockPos> pl : railPolylines) rp.add(Collections.unmodifiableList(new ArrayList<>(pl)));
        }
        this.railPolylines = Collections.unmodifiableList(rp);

        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (StationInfo s : this.stations) {
            BlockPos p = s.pos();
            if (p.getX() < minX) minX = p.getX();
            if (p.getZ() < minZ) minZ = p.getZ();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getZ() > maxZ) maxZ = p.getZ();
        }
        for (ConnectionInfo c : this.connections) {
            BlockPos a = c.from();
            BlockPos b = c.to();
            minX = Math.min(minX, Math.min(a.getX(), b.getX()));
            minZ = Math.min(minZ, Math.min(a.getZ(), b.getZ()));
            maxX = Math.max(maxX, Math.max(a.getX(), b.getX()));
            maxZ = Math.max(maxZ, Math.max(a.getZ(), b.getZ()));
        }
        for (List<BlockPos> pl : this.railPolylines) {
            for (BlockPos p : pl) {
                if (p.getX() < minX) minX = p.getX();
                if (p.getZ() < minZ) minZ = p.getZ();
                if (p.getX() > maxX) maxX = p.getX();
                if (p.getZ() > maxZ) maxZ = p.getZ();
            }
        }
        if (minX == Integer.MAX_VALUE) {
            minX = minZ = 0;
            maxX = maxZ = 1;
        }
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }

    public List<StationInfo> stations() { return stations; }
    public List<ConnectionInfo> connections() { return connections; }
    public List<List<BlockPos>> railPolylines() { return railPolylines; }
    public int minX() { return minX; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxZ() { return maxZ; }

    public int stationCount() { return stations.size(); }
    public int plannedCount() { return (int) connections.stream().filter(c -> c.status() == ConnectionStatus.PLANNED).count(); }
    public int generatingCount() { return (int) connections.stream().filter(c -> c.status() == ConnectionStatus.GENERATING).count(); }
    public int completedCount() { return (int) connections.stream().filter(c -> c.status() == ConnectionStatus.COMPLETED).count(); }

    public static RailwayMapSnapshot empty() {
        return new RailwayMapSnapshot(List.of(), List.of(), List.of());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(stations.size());
        for (StationInfo s : stations) {
            buf.writeBlockPos(s.pos());
            buf.writeUtf(s.name() != null ? s.name() : "");
        }
        buf.writeVarInt(connections.size());
        for (ConnectionInfo c : connections) {
            buf.writeBlockPos(c.from());
            buf.writeBlockPos(c.to());
            buf.writeEnum(c.status());
        }
        buf.writeVarInt(railPolylines.size());
        for (List<BlockPos> pl : railPolylines) {
            buf.writeVarInt(pl.size());
            for (BlockPos p : pl) buf.writeBlockPos(p);
        }
    }

    public static RailwayMapSnapshot decode(FriendlyByteBuf buf) {
        int stationCount = buf.readVarInt();
        List<StationInfo> stations = new ArrayList<>(stationCount);
        for (int i = 0; i < stationCount; i++) {
            BlockPos pos = buf.readBlockPos();
            String name = buf.readUtf();
            stations.add(new StationInfo(pos, name.isEmpty() ? null : name));
        }
        int connCount = buf.readVarInt();
        List<ConnectionInfo> connections = new ArrayList<>(connCount);
        for (int i = 0; i < connCount; i++) {
            BlockPos from = buf.readBlockPos();
            BlockPos to = buf.readBlockPos();
            ConnectionStatus status = buf.readEnum(ConnectionStatus.class);
            connections.add(new ConnectionInfo(from, to, status));
        }
        int polyCount = buf.readVarInt();
        List<List<BlockPos>> polylines = new ArrayList<>(polyCount);
        for (int i = 0; i < polyCount; i++) {
            int len = buf.readVarInt();
            List<BlockPos> pl = new ArrayList<>(len);
            for (int j = 0; j < len; j++) pl.add(buf.readBlockPos());
            polylines.add(pl);
        }
        return new RailwayMapSnapshot(stations, connections, polylines);
    }

    public record StationInfo(BlockPos pos, String name) {}
    public record ConnectionInfo(BlockPos from, BlockPos to, ConnectionStatus status) {}
    public enum ConnectionStatus { PLANNED, GENERATING, COMPLETED }
}
