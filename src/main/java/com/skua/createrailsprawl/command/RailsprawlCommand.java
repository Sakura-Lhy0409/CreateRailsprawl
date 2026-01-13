package com.skua.createrailsprawl.command;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.railway.RailwayBuilder;
import com.skua.createrailsprawl.railway.RailwayMap;
import com.skua.createrailsprawl.railway.RegionPos;
import com.skua.createrailsprawl.railway.planner.StationPlanner;
import com.skua.createrailsprawl.structure.ModStructureManager;
import com.skua.createrailsprawl.util.MyMth;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

public class RailsprawlCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("railsprawl")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("info")
                    .executes(RailsprawlCommand::showInfo))
                .then(Commands.literal("region")
                    .executes(RailsprawlCommand::showRegionStatus))
                .then(Commands.literal("goto")
                    .executes(RailsprawlCommand::teleportToNearestStation))
        );
    }

    private static int showInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        int stationCount = ModStructureManager.normalStation.size() + ModStructureManager.undergroundStation.size();
        int railwayCount = ModStructureManager.ground.size() + ModStructureManager.tunnel.size() + ModStructureManager.bridge.size();

        String info = String.format(
            "=== Create Railsprawl Info ===\n" +
            "Station Templates: %d\n" +
            "Railway Templates: %d\n" +
            "==============================",
            stationCount, railwayCount
        );

        source.sendSuccess(() -> Component.literal(info), false);
        return 1;
    }

    private static int showRegionStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BlockPos playerPos = BlockPos.containing(source.getPosition());
        ChunkPos chunkPos = new ChunkPos(playerPos);
        RegionPos regionPos = MyMth.regionPosFromChunkPos(chunkPos);

        source.sendSuccess(() -> Component.literal(
            "=== Region Status ===\n" +
            "Player Pos: " + playerPos.toShortString() + "\n" +
            "Chunk: " + chunkPos + "\n" +
            "Region: " + regionPos
        ), false);

        RailwayBuilder builder = RailwayBuilder.getInstance(source.getLevel().getSeed());
        if (builder == null) {
            source.sendFailure(Component.literal("RailwayBuilder not initialized!"));
            return 0;
        }

        RailwayMap railwayMap = builder.regionRailways.get(regionPos);
        if (railwayMap == null) {
            source.sendFailure(Component.literal("No railway data for this region!"));
            return 0;
        }

        int stationCount = railwayMap.stations.size();
        int routeChunks = railwayMap.routeMap.size();
        int trackChunks = railwayMap.trackMap.size();

        source.sendSuccess(() -> Component.literal(
            "Stations: " + stationCount + "\n" +
            "Route chunks: " + routeChunks + "\n" +
            "Track chunks: " + trackChunks
        ), false);

        return 1;
    }

    private static int teleportToNearestStation(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BlockPos playerPos = BlockPos.containing(source.getPosition());
        ChunkPos chunkPos = new ChunkPos(playerPos);
        RegionPos regionPos = MyMth.regionPosFromChunkPos(chunkPos);

        RailwayBuilder builder = RailwayBuilder.getInstance(source.getLevel().getSeed());
        if (builder == null) {
            source.sendFailure(Component.literal("RailwayBuilder not initialized!"));
            return 0;
        }

        BlockPos nearestStation = null;
        double nearestDist = Double.MAX_VALUE;

        for (int rx = -1; rx <= 1; rx++) {
            for (int rz = -1; rz <= 1; rz++) {
                RegionPos searchRegion = new RegionPos(regionPos.x() + rx, regionPos.z() + rz);
                RailwayMap railwayMap = builder.regionRailways.get(searchRegion);
                if (railwayMap == null) continue;

                for (StationPlanner.StationGenInfo station : railwayMap.stations) {
                    BlockPos stationPos = station.placePos();
                    double dist = playerPos.distSqr(stationPos);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestStation = stationPos;
                    }
                }
            }
        }

        if (nearestStation == null) {
            source.sendFailure(Component.literal("No stations found in nearby regions!"));
            return 0;
        }

        final BlockPos targetPos = nearestStation;
        try {
            source.getEntityOrException().teleportTo(targetPos.getX() + 0.5, targetPos.getY() + 5, targetPos.getZ() + 0.5);
            source.sendSuccess(() -> Component.literal("Teleported to station at " + targetPos.toShortString()), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to teleport: " + e.getMessage()));
            return 0;
        }
    }
}
