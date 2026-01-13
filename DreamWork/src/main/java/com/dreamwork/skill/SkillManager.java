package com.dreamwork.skill;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * 스킬 관리자
 * 
 * 직업별 스킬의 해금, 쿨다운, 실행을 관리합니다.
 * 
 * @author DreamWork Team
 */
public class SkillManager {

    private final DreamWorkPlugin plugin;

    // 스킬 쿨다운 (UUID:SkillID -> 만료 시간)
    private final Map<String, Long> cooldowns = new HashMap<>();

    public SkillManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 스킬 사용 가능 여부 확인
     */
    public boolean canUseSkill(Player player, JobType jobType, String skillId) {
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int level = userData.getJobLevel(jobType);

        // 해금 레벨 확인
        int unlockLevel = getSkillUnlockLevel(jobType, skillId);
        if (level < unlockLevel) {
            player.sendMessage("§c이 스킬은 " + jobType.getDisplayName() +
                    " Lv." + unlockLevel + "에 해금됩니다.");
            return false;
        }

        // 쿨다운 확인
        if (isOnCooldown(player, skillId)) {
            long remaining = getRemainingCooldown(player, skillId);
            player.sendMessage("§c쿨다운 중입니다. (" + remaining + "초 남음)");
            return false;
        }

        return true;
    }

    /**
     * 스킬 쿨다운 시작
     */
    public void startCooldown(Player player, String skillId, int seconds) {
        String key = player.getUniqueId() + ":" + skillId;
        long expiresAt = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.put(key, expiresAt);
    }

    /**
     * 쿨다운 중인지 확인
     */
    public boolean isOnCooldown(Player player, String skillId) {
        String key = player.getUniqueId() + ":" + skillId;
        Long expiresAt = cooldowns.get(key);

        if (expiresAt == null)
            return false;
        if (System.currentTimeMillis() >= expiresAt) {
            cooldowns.remove(key);
            return false;
        }

        return true;
    }

    /**
     * 남은 쿨다운 시간 (초)
     */
    public long getRemainingCooldown(Player player, String skillId) {
        String key = player.getUniqueId() + ":" + skillId;
        Long expiresAt = cooldowns.get(key);

        if (expiresAt == null)
            return 0;
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /**
     * 스킬 해금 레벨 조회
     */
    public int getSkillUnlockLevel(JobType jobType, String skillId) {
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig(jobType.getConfigKey());
        if (jobConfig == null)
            return 999;

        return jobConfig.getInt("skills." + skillId + ".unlock-level", 999);
    }

    /**
     * 스킬 설정 조회
     */
    public ConfigurationSection getSkillConfig(JobType jobType, String skillId) {
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig(jobType.getConfigKey());
        if (jobConfig == null)
            return null;

        return jobConfig.getConfigurationSection("skills." + skillId);
    }

    /**
     * 스킬 쿨다운 조회
     */
    public int getSkillCooldown(JobType jobType, String skillId) {
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig(jobType.getConfigKey());
        if (jobConfig == null)
            return 60;

        return jobConfig.getInt("skills." + skillId + ".cooldown", 60);
    }

    /**
     * 쿨다운 정리 (메모리 관리)
     */
    public void cleanupCooldowns() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
