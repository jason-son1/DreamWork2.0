package com.dreamwork.core;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 유저 데이터 관리자
 * 
 * SQLite 데이터베이스를 사용하여 플레이어 데이터를 저장/로드합니다.
 * 비동기 처리로 서버 성능에 영향을 최소화합니다.
 * 
 * @author DreamWork Team
 */
public class UserDataManager implements Listener {

    private final DreamWorkPlugin plugin;

    // 데이터베이스 연결
    private Connection connection;

    // 메모리 캐시 (UUID -> UserData)
    private final Map<UUID, UserData> userCache = new ConcurrentHashMap<>();

    // 자동 저장 태스크
    private BukkitTask autoSaveTask;

    public UserDataManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 데이터베이스 초기화
     */
    public void initialize() {
        try {
            // SQLite 데이터베이스 파일 경로
            String dbType = plugin.getConfigManager().getConfig().getString("database.type", "SQLITE");

            if (dbType.equalsIgnoreCase("SQLITE")) {
                String fileName = plugin.getConfigManager().getConfig()
                        .getString("database.sqlite-file", "dreamwork.db");
                File dbFile = new File(plugin.getDataFolder() + "/database", fileName);
                dbFile.getParentFile().mkdirs();

                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

                plugin.debug("SQLite 데이터베이스 연결됨: " + fileName);
            }

            // 테이블 생성
            createTables();

            // 자동 저장 태스크 시작
            startAutoSaveTask();

            // 이미 접속 중인 플레이어 데이터 로드
            for (Player player : Bukkit.getOnlinePlayers()) {
                loadPlayerData(player.getUniqueId());
            }

        } catch (Exception e) {
            plugin.log(Level.SEVERE, "데이터베이스 초기화 실패!");
            e.printStackTrace();
        }
    }

    /**
     * 테이블 생성
     */
    private void createTables() throws SQLException {
        // 유저 직업 데이터 테이블
        String jobDataTable = """
                CREATE TABLE IF NOT EXISTS dw_job_data (
                    uuid VARCHAR(36) NOT NULL,
                    job_type VARCHAR(20) NOT NULL,
                    level INT DEFAULT 1,
                    experience DOUBLE DEFAULT 0,
                    total_exp DOUBLE DEFAULT 0,
                    PRIMARY KEY (uuid, job_type)
                )
                """;

        // 유저 방문 청크 테이블 (탐험가용)
        String visitedChunksTable = """
                CREATE TABLE IF NOT EXISTS dw_visited_chunks (
                    uuid VARCHAR(36) NOT NULL,
                    world VARCHAR(64) NOT NULL,
                    chunk_x INT NOT NULL,
                    chunk_z INT NOT NULL,
                    discovered_at BIGINT DEFAULT 0,
                    PRIMARY KEY (uuid, world, chunk_x, chunk_z)
                )
                """;

        // 유저 미션 진행도 테이블
        String missionProgressTable = """
                CREATE TABLE IF NOT EXISTS dw_mission_progress (
                    uuid VARCHAR(36) NOT NULL,
                    mission_id VARCHAR(64) NOT NULL,
                    progress INT DEFAULT 0,
                    completed BOOLEAN DEFAULT FALSE,
                    assigned_at BIGINT DEFAULT 0,
                    completed_at BIGINT DEFAULT 0,
                    PRIMARY KEY (uuid, mission_id)
                )
                """;

        // 유저 통계 테이블
        String statsTable = """
                CREATE TABLE IF NOT EXISTS dw_stats (
                    uuid VARCHAR(36) NOT NULL,
                    stat_key VARCHAR(64) NOT NULL,
                    stat_value DOUBLE DEFAULT 0,
                    PRIMARY KEY (uuid, stat_key)
                )
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(jobDataTable);
            stmt.execute(visitedChunksTable);
            stmt.execute(missionProgressTable);
            stmt.execute(statsTable);
        }

        plugin.debug("데이터베이스 테이블 생성/확인 완료");
    }

    /**
     * 자동 저장 태스크 시작
     */
    private void startAutoSaveTask() {
        int interval = plugin.getConfigManager().getConfig()
                .getInt("general.auto-save-interval", 300);

        long ticks = interval * 20L; // 초를 틱으로 변환

        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveAllData();
            plugin.debug("자동 저장 완료 (" + userCache.size() + "명)");
        }, ticks, ticks);
    }

    /**
     * 플레이어 데이터 로드
     */
    public void loadPlayerData(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UserData userData = new UserData(uuid);

                // 직업 데이터 로드
                String query = "SELECT job_type, level, experience, total_exp FROM dw_job_data WHERE uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        String jobTypeName = rs.getString("job_type");
                        try {
                            JobType jobType = JobType.valueOf(jobTypeName);
                            int level = rs.getInt("level");
                            double exp = rs.getDouble("experience");
                            double totalExp = rs.getDouble("total_exp");

                            userData.setJobLevel(jobType, level);
                            userData.setJobExp(jobType, exp);
                            userData.setTotalExp(jobType, totalExp);
                        } catch (IllegalArgumentException e) {
                            plugin.debug("알 수 없는 직업 타입: " + jobTypeName);
                        }
                    }
                }

                // 캐시에 저장
                userCache.put(uuid, userData);
                plugin.debug("플레이어 데이터 로드 완료: " + uuid);

            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "플레이어 데이터 로드 실패: " + uuid);
                e.printStackTrace();
            }
        });
    }

    /**
     * 플레이어 데이터 저장
     */
    public void savePlayerData(UUID uuid) {
        UserData userData = userCache.get(uuid);
        if (userData == null)
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 직업 데이터 저장
                String upsert = """
                        INSERT OR REPLACE INTO dw_job_data
                        (uuid, job_type, level, experience, total_exp)
                        VALUES (?, ?, ?, ?, ?)
                        """;

                try (PreparedStatement stmt = connection.prepareStatement(upsert)) {
                    for (JobType jobType : JobType.values()) {
                        stmt.setString(1, uuid.toString());
                        stmt.setString(2, jobType.name());
                        stmt.setInt(3, userData.getJobLevel(jobType));
                        stmt.setDouble(4, userData.getJobExp(jobType));
                        stmt.setDouble(5, userData.getTotalExp(jobType));
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                plugin.debug("플레이어 데이터 저장 완료: " + uuid);

            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "플레이어 데이터 저장 실패: " + uuid);
                e.printStackTrace();
            }
        });
    }

    /**
     * 모든 플레이어 데이터 저장
     */
    public void saveAllData() {
        for (UUID uuid : userCache.keySet()) {
            savePlayerData(uuid);
        }
    }

    /**
     * 종료 처리
     */
    public void shutdown() {
        // 자동 저장 태스크 취소
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        // 모든 데이터 동기적으로 저장
        for (Map.Entry<UUID, UserData> entry : userCache.entrySet()) {
            try {
                savePlayerDataSync(entry.getKey(), entry.getValue());
            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "종료 중 데이터 저장 실패: " + entry.getKey());
            }
        }

        // 데이터베이스 연결 종료
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 동기적 데이터 저장 (종료 시 사용)
     */
    private void savePlayerDataSync(UUID uuid, UserData userData) throws SQLException {
        String upsert = """
                INSERT OR REPLACE INTO dw_job_data
                (uuid, job_type, level, experience, total_exp)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = connection.prepareStatement(upsert)) {
            for (JobType jobType : JobType.values()) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, jobType.name());
                stmt.setInt(3, userData.getJobLevel(jobType));
                stmt.setDouble(4, userData.getJobExp(jobType));
                stmt.setDouble(5, userData.getTotalExp(jobType));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    // ==================== 이벤트 핸들러 ====================

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        savePlayerData(uuid);

        // 약간의 지연 후 캐시에서 제거
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (Bukkit.getPlayer(uuid) == null) {
                userCache.remove(uuid);
            }
        }, 60L); // 3초 후
    }

    // ==================== Getter/Setter 메서드 ====================

    /**
     * 유저 데이터 가져오기
     */
    public UserData getUserData(UUID uuid) {
        return userCache.computeIfAbsent(uuid, UserData::new);
    }

    /**
     * 유저 데이터 가져오기 (플레이어)
     */
    public UserData getUserData(Player player) {
        return getUserData(player.getUniqueId());
    }

    /**
     * 데이터베이스 연결 객체
     */
    public Connection getConnection() {
        return connection;
    }

    // ==================== 청크 방문 관련 메서드 ====================

    /**
     * 청크 방문 여부 확인
     */
    public boolean hasVisitedChunk(UUID uuid, String world, int chunkX, int chunkZ) {
        try {
            String query = "SELECT 1 FROM dw_visited_chunks WHERE uuid = ? AND world = ? AND chunk_x = ? AND chunk_z = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, world);
                stmt.setInt(3, chunkX);
                stmt.setInt(4, chunkZ);
                return stmt.executeQuery().next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 청크 방문 기록
     */
    public void markChunkVisited(UUID uuid, String world, int chunkX, int chunkZ) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String insert = "INSERT OR IGNORE INTO dw_visited_chunks (uuid, world, chunk_x, chunk_z, discovered_at) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(insert)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, world);
                    stmt.setInt(3, chunkX);
                    stmt.setInt(4, chunkZ);
                    stmt.setLong(5, System.currentTimeMillis());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
