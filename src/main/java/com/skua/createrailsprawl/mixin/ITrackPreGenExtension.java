/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;

/**
 * TrackBlockEntity扩展接口
 * 用于在世界生成阶段预添加BezierConnection
 */
public interface ITrackPreGenExtension {
    /**
     * 添加预生成的连接
     * 连接会在BlockEntity的tick中被正式添加
     */
    void addConnectionToPreGen(BezierConnection connection);
}
