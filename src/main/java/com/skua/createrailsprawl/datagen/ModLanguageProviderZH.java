package com.skua.createrailsprawl.datagen;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.block.ModBlocks;
import com.skua.createrailsprawl.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModLanguageProviderZH extends LanguageProvider {
    public ModLanguageProviderZH(PackOutput output) {
        super(output, CreateRailsprawl.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        // 物品和方块
        add(ModItems.RAILWAY_BLUEPRINT.get(), "铁轨蓝图");
        add(ModBlocks.TRACK_SPAWNER.get(), "轨道生成器");
        add("itemGroup.createrailsprawl", "机械动力·铁轨蔓延");

        // 命令
        add("command.createrailsprawl.generate.start", "开始生成铁轨网络，任务ID: %s，半径: %d 区块");
        add("command.createrailsprawl.generate.progress", "生成中...已完成 %d/%d 区块");
        add("command.createrailsprawl.generate.complete", "生成完成！%s");
        add("command.createrailsprawl.generate.cooldown", "生成冷却中，请稍后再试");
        add("command.createrailsprawl.generate.limit", "生成次数已达上限，请稍后再试");
        add("command.createrailsprawl.remove.success", "已删除 %d 个区块的铁轨数据");
        add("command.createrailsprawl.reload.success", "配置已重载");
        add("command.createrailsprawl.rollback.success", "已回滚 %d 个区块");
        add("command.createrailsprawl.stats.title", "=== 铁轨网络统计 ===");
        add("command.createrailsprawl.stats.track_length", "总轨道长度: %d 格");
        add("command.createrailsprawl.stats.chunks", "已生成区块: %d 个");
        add("command.createrailsprawl.stats.memory", "内存占用: %d KB");
        add("command.createrailsprawl.stats.queue", "任务队列: %d 个");
        add("command.createrailsprawl.emergency.stop", "紧急停止！所有生成任务已取消");
        add("command.createrailsprawl.emergency.resume", "任务处理已恢复");
        add("command.createrailsprawl.debug.on", "调试模式已开启（显示粒子效果）");
        add("command.createrailsprawl.debug.off", "调试模式已关闭");
        add("command.createrailsprawl.hud.toggle", "HUD显示已切换");

        // GUI
        add("gui.createrailsprawl.editor.title", "铁轨生成编辑器");
        add("gui.createrailsprawl.editor.radius", "生成半径:");
        add("gui.createrailsprawl.editor.width", "轨道宽度:");
        add("gui.createrailsprawl.editor.slope", "坡度角度:");
        add("gui.createrailsprawl.editor.bridge", "桥梁:");
        add("gui.createrailsprawl.editor.tunnel", "隧道:");
        add("gui.createrailsprawl.editor.target", "目标结构:");
        add("gui.createrailsprawl.editor.generate", "立即生成");
        add("gui.createrailsprawl.editor.preview", "预览");
        add("gui.createrailsprawl.editor.cancel", "取消");

        // 结构
        add("gui.createrailsprawl.structure.village", "村庄");
        add("gui.createrailsprawl.structure.mineshaft", "矿井");
        add("gui.createrailsprawl.structure.stronghold", "要塞");
        add("gui.createrailsprawl.structure.desert_temple", "沙漠神殿");
        add("gui.createrailsprawl.structure.jungle_temple", "丛林神庙");
        add("gui.createrailsprawl.structure.ocean_monument", "海底神殿");
        add("gui.createrailsprawl.structure.custom", "自定义标记");

        // HUD
        add("hud.createrailsprawl.generating", "铁轨生成中");
        add("hud.createrailsprawl.progress", "%d/%d 区块");
        add("hud.createrailsprawl.building", "当前构建: %s");
        add("hud.createrailsprawl.complete", "生成完成");

        // 地形
        add("terrain.createrailsprawl.flat", "平地");
        add("terrain.createrailsprawl.tunnel", "隧道");
        add("terrain.createrailsprawl.bridge", "桥梁");
        add("terrain.createrailsprawl.viaduct", "高架桥");

        // 消息
        add("message.createrailsprawl.blueprint.activate", "铁轨蓝图激活！开始生成铁轨网络...");
        add("message.createrailsprawl.blueprint.cooldown", "蓝图冷却中，还需等待 %d 秒");
        add("message.createrailsprawl.complete", "铁轨网络生成完成！共创建 %d 段轨道 + %d 个弧形转弯 + %d 个站台");

        // 快捷键
        add("key.createrailsprawl.open_editor", "打开铁轨编辑器");
        add("key.createrailsprawl.toggle_hud", "切换铁轨HUD");
        add("key.categories.createrailsprawl", "机械动力·铁轨蔓延");
    }
}
