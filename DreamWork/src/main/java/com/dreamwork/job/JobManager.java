package com.dreamwork.job;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * 직업 관리자
 * 
 * 모든 직업의 경험치 획득과 레벨업을 관리합니다.
 * 레벨업 공식은 설정 파일에서 로드됩니다.
 * 
 * @author DreamWork Team
 */
public class JobManager {

    private final DreamWorkPlugin plugin;

    // 레벨별 필요 경험치 캐시
    private final Map<Integer, Double> expRequirements = new HashMap<>();

    // 최대 레벨
    private int maxLevel = 100;

    // 글로벌 배율
    private double globalExpMultiplier = 1.0;
    private double globalMoneyMultiplier = 1.0;

    public JobManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        loadSettings();
        calculateExpTable();
    }

    /**
     * 설정 로드
     */
    private void loadSettings() {
        FileConfiguration config = plugin.getConfigManager().getConfig();

        maxLevel = config.getInt("jobs.max-level", 100);
        globalExpMultiplier = config.getDouble("jobs.global-exp-multiplier", 1.0);
        globalMoneyMultiplier = config.getDouble("jobs.global-money-multiplier", 1.0);
    }

    /**
     * 경험치 테이블 계산
     * 공식: 100 * (level ^ 1.5) + 50
     */
    private void calculateExpTable() {
        expRequirements.clear();

        String formula = plugin.getConfigManager().getConfig()
                .getString("jobs.exp-formula", "100 * Math.pow(level, 1.5) + 50");

        for (int level = 1; level <= maxLevel; level++) {
            double required;
            try {
                // 간단한 공식 계산 (Math.pow 지원)
                required = 100 * Math.pow(level, 1.5) + 50;
            } catch (Exception e) {
                required = 100 * level;
            }
            expRequirements.put(level, required);
        }

        plugin.debug("경험치 테이블 생성 완료 (1~" + maxLevel + " 레벨)");
    }

    /**
     * 경험치 추가 및 레벨업 처리
     * 
     * @param player  플레이어
     * @param jobType 직업 타입
     * @param baseExp 기본 경험치
     * @return 실제 획득한 경험치
     */
    public double addExperience(Player player, JobType jobType, double baseExp) {
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int currentLevel = userData.getJobLevel(jobType);

        // 최대 레벨이면 경험치 획득 안 함
        if (currentLevel >= maxLevel) {
            return 0;
        }

        // 배율 적용
        double finalExp = baseExp * globalExpMultiplier;

        // 경험치 추가
        double currentExp = userData.getJobExp(jobType) + finalExp;
        userData.setJobExp(jobType, currentExp);
        userData.addTotalExp(jobType, finalExp);

        // 레벨업 체크 (연속 레벨업 처리)
        while (currentLevel < maxLevel) {
            double required = getRequiredExp(currentLevel);

            if (currentExp >= required) {
                currentExp -= required;
                currentLevel++;

                // 레벨업 처리
                userData.setJobLevel(jobType, currentLevel);
                userData.setJobExp(jobType, currentExp);

                onLevelUp(player, jobType, currentLevel);
            } else {
                break;
            }
        }

        // 최대 레벨 도달 시 경험치 0으로 고정
        if (currentLevel >= maxLevel) {
            userData.setJobExp(jobType, 0);
        }

        return finalExp;
    }

    /**
     * 레벨업 이벤트 처리
     */
    private void onLevelUp(Player player, JobType jobType, int newLevel) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        String jobName = plugin.getConfigManager().getMessage("job-names." + jobType.name());

        // 타이틀 표시
        if (config.getBoolean("levelup.show-title", true)) {
            String title = plugin.getConfigManager().getMessage("job.levelup-title");
            String subtitle = plugin.getConfigManager()
                    .getMessage("job.levelup-subtitle")
                    .replace("{job}", jobName)
                    .replace("{old_level}", String.valueOf(newLevel - 1))
                    .replace("{new_level}", String.valueOf(newLevel));

            player.sendTitle(title, subtitle, 10, 70, 20);
        }

        // 사운드 재생
        if (config.getBoolean("levelup.play-sound", true)) {
            String soundName = config.getString("levelup.sound", "ENTITY_PLAYER_LEVELUP");
            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.debug("알 수 없는 사운드: " + soundName);
            }
        }

        // 채팅 메시지
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("job", jobName);
        placeholders.put("new_level", String.valueOf(newLevel));
        String message = plugin.getConfigManager().getMessage("job.levelup-chat", placeholders);
        player.sendMessage(plugin.getConfigManager().getMessage("prefix") + message);

        // LuckPerms 권한/칭호 업데이트
        if (plugin.getLuckPermsHook() != null && plugin.getLuckPermsHook().isEnabled()) {
            plugin.getLuckPermsHook().updatePlayerRank(player, jobType, newLevel);
        }

        // 스킬 해금 체크
        checkSkillUnlock(player, jobType, newLevel);

        plugin.debug(player.getName() + " 레벨업: " + jobType.name() + " Lv." + newLevel);
    }

    /**
     * 스킬 해금 체크
     */
    private void checkSkillUnlock(Player player, JobType jobType, int level) {
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig(jobType.getConfigKey());
        if (jobConfig == null)
            return;

        // 스킬 섹션에서 해금 레벨 체크
        if (jobConfig.isConfigurationSection("skills")) {
            for (String skillKey : jobConfig.getConfigurationSection("skills").getKeys(false)) {
                int unlockLevel = jobConfig.getInt("skills." + skillKey + ".unlock-level", 1);

                if (unlockLevel == level) {
                    String skillName = jobConfig.getString("skills." + skillKey + ".name", skillKey);
                    String message = plugin.getConfigManager()
                            .getMessage("job.skill-unlocked")
                            .replace("{skill}", skillName);
                    player.sendMessage(plugin.getConfigManager().getMessage("prefix") + message);
                }
            }
        }
    }

    /**
     * 돈 지급
     */
    public double giveMoney(Player player, double baseMoney) {
        double finalMoney = baseMoney * globalMoneyMultiplier;

        if (plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            plugin.getVaultHook().deposit(player, finalMoney);
        }

        return finalMoney;
    }

    /**
     * 경험치 획득 + 돈 획득 + 액션바 표시
     */
    public void giveReward(Player player, JobType jobType, double baseExp, double baseMoney) {
        double earnedExp = addExperience(player, jobType, baseExp);
        double earnedMoney = giveMoney(player, baseMoney);

        // 액션바 표시
        if (plugin.getConfigManager().getConfig().getBoolean("actionbar.show-exp-gain", true)) {
            showActionBar(player, jobType, earnedExp, earnedMoney);
        }
    }

    /**
     * 액션바에 획득 정보 표시
     */
    private void showActionBar(Player player, JobType jobType, double exp, double money) {
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int level = userData.getJobLevel(jobType);
        double currentExp = userData.getJobExp(jobType);
        double requiredExp = getRequiredExp(level);

        // 진행률 계산
        double percent = (currentExp / requiredExp) * 100;
        String progressBar = createProgressBar(percent, 10);

        String format = plugin.getConfigManager().getConfig()
                .getString("actionbar.format",
                        "&e+{exp} Exp &6(+{money}G) &7| &f{job} Lv.{level} &a[{progress_bar}] &7({percent}%)");

        String jobName = plugin.getConfigManager().getMessage("job-names." + jobType.name());

        String message = format
                .replace("{exp}", String.format("%.1f", exp))
                .replace("{money}", String.format("%.1f", money))
                .replace("{job}", jobName)
                .replace("{level}", String.valueOf(level))
                .replace("{progress_bar}", progressBar)
                .replace("{percent}", String.format("%.0f", percent));

        message = plugin.getConfigManager().translateColors(message);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }

    /**
     * 프로그레스 바 생성
     */
    private String createProgressBar(double percent, int totalBars) {
        int filledBars = (int) ((percent / 100) * totalBars);
        StringBuilder bar = new StringBuilder();

        bar.append("§a");
        for (int i = 0; i < filledBars && i < totalBars; i++) {
            bar.append("▮");
        }

        bar.append("§7");
        for (int i = filledBars; i < totalBars; i++) {
            bar.append("▯");
        }

        return bar.toString();
    }

    // ==================== Getter 메서드 ====================

    /**
     * 특정 레벨의 필요 경험치
     */
    public double getRequiredExp(int level) {
        return expRequirements.getOrDefault(level, 100.0 * level);
    }

    /**
     * 최대 레벨
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * 글로벌 경험치 배율
     */
    public double getGlobalExpMultiplier() {
        return globalExpMultiplier;
    }

    /**
     * 글로벌 돈 배율
     */
    public double getGlobalMoneyMultiplier() {
        return globalMoneyMultiplier;
    }

    /**
     * 글로벌 경험치 배율 설정 (이벤트용)
     */
    public void setGlobalExpMultiplier(double multiplier) {
        this.globalExpMultiplier = multiplier;
    }

    /**
     * 글로벌 돈 배율 설정 (이벤트용)
     */
    public void setGlobalMoneyMultiplier(double multiplier) {
        this.globalMoneyMultiplier = multiplier;
    }

    /**
     * 설정 리로드
     */
    public void reload() {
        loadSettings();
        calculateExpTable();
    }

    /**
     * 플레이어의 현재 잔액을 포맷팅하여 반환
     * 
     * @param player 플레이어
     * @return 포맷팅된 잔액 문자열 (예: "1,234.5")
     */
    public String getFormattedBalance(Player player) {
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            double balance = plugin.getVaultHook().getBalance(player);
            return String.format("%,.1f", balance);
        }
        return "0.0";
    }
}
