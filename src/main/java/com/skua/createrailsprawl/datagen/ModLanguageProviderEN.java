package com.skua.createrailsprawl.datagen;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.block.ModBlocks;
import com.skua.createrailsprawl.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModLanguageProviderEN extends LanguageProvider {
    public ModLanguageProviderEN(PackOutput output) {
        super(output, CreateRailsprawl.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // 物品和方块
        add(ModItems.RAILWAY_BLUEPRINT.get(), "Railway Blueprint");
        add(ModBlocks.TRACK_SPAWNER.get(), "Track Spawner");
        add("itemGroup.createrailsprawl", "Create Railsprawl");

        // 命令
        add("command.createrailsprawl.generate.start", "Starting railway network generation, Task ID: %s, Radius: %d chunks");
        add("command.createrailsprawl.generate.progress", "Generating... %d/%d chunks completed");
        add("command.createrailsprawl.generate.complete", "Generation complete! %s");
        add("command.createrailsprawl.generate.cooldown", "Generation on cooldown, please wait");
        add("command.createrailsprawl.generate.limit", "Generation limit reached, please wait");
        add("command.createrailsprawl.remove.success", "Removed railway data from %d chunks");
        add("command.createrailsprawl.reload.success", "Configuration reloaded");
        add("command.createrailsprawl.rollback.success", "Rolled back %d chunks");
        add("command.createrailsprawl.stats.title", "=== Railway Network Statistics ===");
        add("command.createrailsprawl.stats.track_length", "Total track length: %d blocks");
        add("command.createrailsprawl.stats.chunks", "Generated chunks: %d");
        add("command.createrailsprawl.stats.memory", "Memory usage: %d KB");
        add("command.createrailsprawl.stats.queue", "Task queue: %d");
        add("command.createrailsprawl.emergency.stop", "Emergency stop! All generation tasks cancelled");
        add("command.createrailsprawl.emergency.resume", "Task processing resumed");
        add("command.createrailsprawl.debug.on", "Debug mode enabled (showing particle effects)");
        add("command.createrailsprawl.debug.off", "Debug mode disabled");
        add("command.createrailsprawl.hud.toggle", "HUD display toggled");

        // GUI
        add("gui.createrailsprawl.editor.title", "Railway Generation Editor");
        add("gui.createrailsprawl.editor.radius", "Generation Radius:");
        add("gui.createrailsprawl.editor.width", "Track Width:");
        add("gui.createrailsprawl.editor.slope", "Slope Angle:");
        add("gui.createrailsprawl.editor.bridge", "Bridge:");
        add("gui.createrailsprawl.editor.tunnel", "Tunnel:");
        add("gui.createrailsprawl.editor.target", "Target Structure:");
        add("gui.createrailsprawl.editor.generate", "Generate Now");
        add("gui.createrailsprawl.editor.preview", "Preview");
        add("gui.createrailsprawl.editor.cancel", "Cancel");

        // 结构
        add("gui.createrailsprawl.structure.village", "Village");
        add("gui.createrailsprawl.structure.mineshaft", "Mineshaft");
        add("gui.createrailsprawl.structure.stronghold", "Stronghold");
        add("gui.createrailsprawl.structure.desert_temple", "Desert Temple");
        add("gui.createrailsprawl.structure.jungle_temple", "Jungle Temple");
        add("gui.createrailsprawl.structure.ocean_monument", "Ocean Monument");
        add("gui.createrailsprawl.structure.custom", "Custom Marker");

        // HUD
        add("hud.createrailsprawl.generating", "Generating Railway");
        add("hud.createrailsprawl.progress", "%d/%d chunks");
        add("hud.createrailsprawl.building", "Building: %s");
        add("hud.createrailsprawl.complete", "Generation Complete");

        // 地形
        add("terrain.createrailsprawl.flat", "Flat");
        add("terrain.createrailsprawl.tunnel", "Tunnel");
        add("terrain.createrailsprawl.bridge", "Bridge");
        add("terrain.createrailsprawl.viaduct", "Viaduct");

        // 消息
        add("message.createrailsprawl.blueprint.activate", "Railway Blueprint activated! Starting network generation...");
        add("message.createrailsprawl.blueprint.cooldown", "Blueprint on cooldown, %d seconds remaining");
        add("message.createrailsprawl.complete", "Railway network complete! Created %d track segments + %d curves + %d stations");

        // 快捷键
        add("key.createrailsprawl.open_editor", "Open Railway Editor");
        add("key.createrailsprawl.toggle_hud", "Toggle Railway HUD");
        add("key.categories.createrailsprawl", "Create Railsprawl");
    }
}
