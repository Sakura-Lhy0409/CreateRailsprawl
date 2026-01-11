package com.skua.createrailsprawl.persistence;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.railway.RegionPos;
import net.minecraft.server.level.ServerLevel;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 铁路数据分片存储 - SQLite 实现
 * 用于大规模铁路网络的高效存储
 */
public final class RailwayShardStorage {
    private RailwayShardStorage() {}

    private static final Map<String, Connection> CONNECTIONS = new ConcurrentHashMap<>();
    private static final String DB_NAME = "railway_data.db";

    /**
     * 获取或创建数据库连接
     */
    public static Connection getConnection(ServerLevel level) {
        String dimKey = level.dimension().location().toString();
        return CONNECTIONS.computeIfAbsent(dimKey, k -> {
            try {
                File worldDir = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
                File dbFile = new File(worldDir, "createrailsprawl/" + DB_NAME);
                dbFile.getParentFile().mkdirs();

                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                Connection conn = DriverManager.getConnection(url);
                initTables(conn);
                return conn;
            } catch (SQLException e) {
                CreateRailsprawl.LOGGER.error("无法创建数据库连接: {}", e.getMessage());
                return null;
            }
        });
    }

    private static void initTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS railway_regions (
                    region_x INTEGER NOT NULL,
                    region_z INTEGER NOT NULL,
                    data BLOB,
                    updated_at INTEGER DEFAULT (strftime('%s', 'now')),
                    PRIMARY KEY (region_x, region_z)
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_region ON railway_regions(region_x, region_z)");
        }
    }

    /**
     * 保存区域数据
     */
    public static void saveRegion(ServerLevel level, RegionPos pos, byte[] data) {
        Connection conn = getConnection(level);
        if (conn == null) return;

        String sql = "INSERT OR REPLACE INTO railway_regions (region_x, region_z, data) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, pos.x());
            pstmt.setInt(2, pos.z());
            pstmt.setBytes(3, data);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            CreateRailsprawl.LOGGER.error("保存区域数据失败: {}", e.getMessage());
        }
    }

    /**
     * 加载区域数据
     */
    public static byte[] loadRegion(ServerLevel level, RegionPos pos) {
        Connection conn = getConnection(level);
        if (conn == null) return null;

        String sql = "SELECT data FROM railway_regions WHERE region_x = ? AND region_z = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, pos.x());
            pstmt.setInt(2, pos.z());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("data");
                }
            }
        } catch (SQLException e) {
            CreateRailsprawl.LOGGER.error("加载区域数据失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 删除区域数据
     */
    public static void deleteRegion(ServerLevel level, RegionPos pos) {
        Connection conn = getConnection(level);
        if (conn == null) return;

        String sql = "DELETE FROM railway_regions WHERE region_x = ? AND region_z = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, pos.x());
            pstmt.setInt(2, pos.z());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            CreateRailsprawl.LOGGER.error("删除区域数据失败: {}", e.getMessage());
        }
    }

    /**
     * 关闭指定维度的连接
     */
    public static void closeConnection(ServerLevel level) {
        String dimKey = level.dimension().location().toString();
        Connection conn = CONNECTIONS.remove(dimKey);
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                CreateRailsprawl.LOGGER.error("关闭数据库连接失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 关闭所有连接
     */
    public static void shutdown() {
        for (Connection conn : CONNECTIONS.values()) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                CreateRailsprawl.LOGGER.error("关闭数据库连接失败: {}", e.getMessage());
            }
        }
        CONNECTIONS.clear();
    }
}
