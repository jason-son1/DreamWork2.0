package com.dreamwork.mission;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;

/**
 * 미션 관리자
 * 
 * 미션 템플릿을 로드하고 플레이어의 미션 진행도를 관리합니다.
 * 
 * @author DreamWork Team
 */
public class MissionManager {

    private final DreamWorkPlugin plugin;

    // 미션 템플릿 캐시 (ID -> Template)
    private final Map<String, MissionTemplate> missionCache = new HashMap<>();

    public MissionManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 모든 미션 로드
     */
    public void loadMissions() {
        missionCache.clear();

        Map<String, FileConfiguration> missionConfigs = plugin.getConfigManager().getAllMissionConfigs();

        for (Map.Entry<String, FileConfiguration> entry : missionConfigs.entrySet()) {
            String fileName = entry.getKey();
            FileConfiguration config = entry.getValue();

            if (config == null)
                continue;

            // 미션 섹션 로드
            for (String missionId : config.getKeys(false)) {
                if (config.isConfigurationSection(missionId)) {
                    loadMission(missionId, config.getConfigurationSection(missionId));
                }
            }
        }

        plugin.log(Level.INFO, "미션 " + missionCache.size() + "개 로드 완료");
    }

    /**
     * 개별 미션 로드
     */
    private void loadMission(String id, ConfigurationSection section) {
        if (section == null)
            return;

        try {
            MissionTemplate template = new MissionTemplate();
            template.setId(id);

            // 기본 정보
            template.setDisplayName(translateColors(section.getString("display_name", id)));
            template.setType(MissionType.valueOf(
                    section.getString("type", "BREAK").toUpperCase()));
            template.setTargets(section.getStringList("target"));
            template.setAmount(section.getInt("amount", 1));

            // 조건
            template.setConditions(section.getStringList("conditions"));

            // 보상
            ConfigurationSection rewardsSection = section.getConfigurationSection("rewards");
            if (rewardsSection != null) {
                template.setRewardMoney(rewardsSection.getDouble("money", 0));
                template.setRewardItems(rewardsSection.getStringList("items"));

                ConfigurationSection jobExpSection = rewardsSection.getConfigurationSection("job_exp");
                if (jobExpSection != null) {
                    Map<String, Double> jobExp = new HashMap<>();
                    for (String job : jobExpSection.getKeys(false)) {
                        jobExp.put(job, jobExpSection.getDouble(job));
                    }
                    template.setRewardJobExp(jobExp);
                }
            }

            // 연계 미션
            template.setNextMission(section.getString("next_mission"));

            // 초기화 주기
            String resetCycle = section.getString("reset_cycle", "ONE_TIME");
            template.setResetCycle(resetCycle);

            missionCache.put(id, template);
            plugin.debug("미션 로드됨: " + id);

        } catch (Exception e) {
            plugin.log(Level.WARNING, "미션 로드 실패: " + id + " - " + e.getMessage());
        }
    }

    /**
     * 미션 수락
     */
    public void acceptMission(org.bukkit.entity.Player player, String missionId) {
        MissionTemplate template = getMission(missionId);
        if (template == null)
            return;

        com.dreamwork.core.UserData userData = plugin.getUserDataManager().getUserData(player);
        com.dreamwork.mission.PlayerMissionData data = userData.getOrCreateMission(missionId);

        // 이미 시작했거나 완료한 경우 무시
        if (data.getStatus() != com.dreamwork.mission.MissionStatus.NOT_STARTED) {
            return;
        }

        data.setStatus(com.dreamwork.mission.MissionStatus.IN_PROGRESS);
        player.sendMessage("§a[미션] §f" + template.getDisplayName() + " §a미션을 수락했습니다!");
    }

    /**
     * 미션 이벤트 처리
     * 직업 리스너에서 호출됩니다.
     */
    public void processEvent(org.bukkit.entity.Player player, MissionType type, String target, int amount) {
        com.dreamwork.core.UserData userData = plugin.getUserDataManager().getUserData(player);

        // 진행 중인 모든 미션 확인
        for (com.dreamwork.mission.PlayerMissionData data : userData.getAllMissions().values()) {
            if (data.getStatus() != com.dreamwork.mission.MissionStatus.IN_PROGRESS) {
                continue;
            }

            MissionTemplate template = getMission(data.getMissionId());
            if (template == null)
                continue;

            // 미션 타입 일치 확인
            if (template.getType() != type)
                continue;

            // 타겟 일치 확인
            if (!template.getTargets().contains(target) && !template.getTargets().contains("any")) {
                continue;
            }

            // 진행도 업데이트
            userData.updateMissionProgress(data.getMissionId(), amount, template.getAmount());

            // 완료 체크 (UserData.updateMissionProgress에서 상태 변경됨)
            if (data.getStatus() == com.dreamwork.mission.MissionStatus.COMPLETED) {
                // 완료 알림
                player.sendTitle("§a미션 완료!", "§f" + template.getDisplayName(), 10, 70, 20);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                player.sendMessage("§a[미션] §f" + template.getDisplayName() + " §7완료! 보상을 수령하세요.");

                // 자동 보상 지급 (설정에 따라 변경 가능)
                if (true) {
                    completeMission(player, data.getMissionId());
                }
            }
        }
    }

    /**
     * 미션 완료 및 보상 지급
     */
    public void completeMission(org.bukkit.entity.Player player, String missionId) {
        com.dreamwork.core.UserData userData = plugin.getUserDataManager().getUserData(player);
        com.dreamwork.mission.PlayerMissionData data = userData.getMission(missionId);

        if (data == null || data.getStatus() != com.dreamwork.mission.MissionStatus.COMPLETED) {
            return;
        }

        MissionTemplate template = getMission(missionId);
        if (template == null)
            return;

        // 보상 지급
        if (template.getRewardMoney() > 0) {
            plugin.getJobManager().giveMoney(player, template.getRewardMoney());
            player.sendMessage("§e💰 보상: §f" + template.getRewardMoney() + "G");
        }

        // 아이템 보상
        for (String itemId : template.getRewardItems()) {
            org.bukkit.inventory.ItemStack item = plugin.getItemManager().createItem(itemId, 1);
            if (item != null) {
                player.getInventory().addItem(item);
                player.sendMessage("§e🎁 보상: §f"
                        + (item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : itemId));
            }
        }

        // 직업 경험치 보상
        if (template.getRewardJobExp() != null) {
            for (Map.Entry<String, Double> entry : template.getRewardJobExp().entrySet()) {
                com.dreamwork.job.JobType jobType = com.dreamwork.job.JobType.fromConfigKey(entry.getKey());
                if (jobType != null) {
                    plugin.getJobManager().addExperience(player, jobType, entry.getValue());
                    player.sendMessage("§e✨ 보상: §f" + jobType.getDisplayName() + " 경험치 +" + entry.getValue());
                }
            }
        }

        // 상태 변경
        data.setStatus(com.dreamwork.mission.MissionStatus.CLAIMED);

        // 연계 미션 자동 수락
        if (template.getNextMission() != null) {
            acceptMission(player, template.getNextMission());
        }
    }

    /**
     * 미션 템플릿 가져오기
     */
    public MissionTemplate getMission(String id) {
        return missionCache.get(id);
    }

    /**
     * 모든 미션 ID 목록
     */
    public Set<String> getAllMissionIds() {
        return new HashSet<>(missionCache.keySet());
    }

    /**
     * 색상 코드 변환
     */
    private String translateColors(String text) {
        if (text == null)
            return "";
        return text.replace("&", "§");
    }
}
