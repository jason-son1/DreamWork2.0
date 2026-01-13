package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 광부의 몰입 패시브 (광부)
 * 
 * 한 지역(청크)에서 오래 채굴할수록 경험치와 드롭률이 증가합니다.
 * 청크를 벗어나거나 5분 이상 채굴하지 않으면 몰입이 해제됩니다.
 * 
 * 단계:
 * - 1단계 (10분): 경험치 +10%
 * - 2단계 (20분): 경험치 +20%, 드롭률 +10%
 * - 3단계 (30분+): 경험치 +30%, 드롭률 +20%, 희귀 아이템 확률 +5%
 * 
 * @author DreamWork Team
 */
public class MinersTrance implements Listener {

    private final DreamWorkPlugin plugin;

    // 플레이어별 몰입 데이터
    private final Map<UUID, TranceData> tranceDataMap = new HashMap<>();

    public MinersTrance(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 블록 파괴 시 몰입 상태 업데이트
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // 광부 레벨 확인
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int minerLevel = userData.getJobLevel(JobType.MINER);

        int unlockLevel = plugin.getConfigManager().getJobConfig("miner")
                .getInt("skills.miners_trance.unlock-level", 50);

        if (minerLevel < unlockLevel) {
            return;
        }

        // 현재 청크
        Chunk currentChunk = player.getLocation().getChunk();
        UUID uuid = player.getUniqueId();

        TranceData data = tranceDataMap.computeIfAbsent(uuid, k -> new TranceData());

        long now = System.currentTimeMillis();

        // 청크가 바뀌었거나 5분 이상 채굴하지 않았으면 리셋
        if (!data.isSameChunk(currentChunk) || now - data.lastMineTime > 300_000) {
            data.reset(currentChunk, now);
            player.sendMessage("§7⚒ 광부의 몰입 시작...");
            plugin.debug(player.getName() + " 몰입 시작");
        } else {
            // 몰입 지속
            data.lastMineTime = now;

            int stage = data.getCurrentStage();
            int previousStage = data.previousStage;

            // 단계가 올라갔을 때 알림
            if (stage > previousStage) {
                data.previousStage = stage;
                String stageName = getStageName(stage);
                double expBonus = getExpBonus(stage);
                double dropBonus = getDropBonus(stage);

                String message = String.format(
                        "§6⚒ 광부의 몰입 %s 달성! §e(경험치 +%d%%",
                        stageName,
                        (int) (expBonus * 100));

                if (dropBonus > 0) {
                    message += ", 드롭률 +" + (int) (dropBonus * 100) + "%";
                }

                message += ")";
                player.sendMessage(message);

                plugin.debug(player.getName() + " 몰입 " + stageName + " 달성");
            }
        }
    }

    /**
     * 플레이어 퇴장 시 몰입 데이터 정리
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        tranceDataMap.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 현재 몰입 단계의 경험치 보너스 가져오기
     */
    public double getExpBonus(Player player) {
        TranceData data = tranceDataMap.get(player.getUniqueId());
        if (data == null)
            return 0.0;

        return getExpBonus(data.getCurrentStage());
    }

    /**
     * 현재 몰입 단계의 드롭 보너스 가져오기
     */
    public double getDropBonus(Player player) {
        TranceData data = tranceDataMap.get(player.getUniqueId());
        if (data == null)
            return 0.0;

        return getDropBonus(data.getCurrentStage());
    }

    /**
     * 현재 몰입 단계의 희귀 아이템 확률 보너스 가져오기
     */
    public double getRareChanceBonus(Player player) {
        TranceData data = tranceDataMap.get(player.getUniqueId());
        if (data == null)
            return 0.0;

        return getRareChanceBonus(data.getCurrentStage());
    }

    /**
     * 단계별 경험치 보너스
     */
    private double getExpBonus(int stage) {
        return switch (stage) {
            case 1 -> 0.10; // 10%
            case 2 -> 0.20; // 20%
            case 3 -> 0.30; // 30%
            default -> 0.0;
        };
    }

    /**
     * 단계별 드롭 보너스
     */
    private double getDropBonus(int stage) {
        return switch (stage) {
            case 2 -> 0.10; // 10%
            case 3 -> 0.20; // 20%
            default -> 0.0;
        };
    }

    /**
     * 단계별 희귀 아이템 확률 보너스
     */
    private double getRareChanceBonus(int stage) {
        return stage == 3 ? 0.05 : 0.0; // 3단계만 5%
    }

    /**
     * 단계 이름
     */
    private String getStageName(int stage) {
        return switch (stage) {
            case 1 -> "1단계";
            case 2 -> "2단계";
            case 3 -> "3단계";
            default -> "";
        };
    }

    /**
     * 몰입 데이터 클래스
     */
    private static class TranceData {
        String chunkKey;
        long startTime;
        long lastMineTime;
        int previousStage = 0;

        void reset(Chunk chunk, long now) {
            this.chunkKey = getChunkKey(chunk);
            this.startTime = now;
            this.lastMineTime = now;
            this.previousStage = 0;
        }

        boolean isSameChunk(Chunk chunk) {
            return getChunkKey(chunk).equals(this.chunkKey);
        }

        int getCurrentStage() {
            long duration = lastMineTime - startTime;
            long minutes = duration / 60_000;

            if (minutes >= 30)
                return 3;
            if (minutes >= 20)
                return 2;
            if (minutes >= 10)
                return 1;
            return 0;
        }

        private String getChunkKey(Chunk chunk) {
            return chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
        }
    }
}
