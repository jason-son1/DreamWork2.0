package com.dreamwork.mission;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;

/**
 * 미션 로더
 * 
 * YAML 설정 파일에서 미션 및 미션 체인을 로드합니다.
 * 
 * @author DreamWork Team
 */
public class MissionLoader {

    private final DreamWorkPlugin plugin;

    public MissionLoader(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 모든 미션 로드
     */
    public Map<String, MissionTemplate> loadAllMissions() {
        Map<String, MissionTemplate> loadedMissions = new HashMap<>();
        Map<String, FileConfiguration> missionConfigs = plugin.getConfigManager().getAllMissionConfigs();

        for (Map.Entry<String, FileConfiguration> entry : missionConfigs.entrySet()) {
            FileConfiguration config = entry.getValue();
            if (config == null)
                continue;

            // 1. 일반 미션 로드 (루트 섹션에 있는 경우)
            for (String key : config.getKeys(false)) {
                if (config.isConfigurationSection(key) && !key.equals("chains")) {
                    MissionTemplate mission = loadSingleMission(key, config.getConfigurationSection(key));
                    if (mission != null) {
                        loadedMissions.put(key, mission);
                    }
                }
            }

            // 2. 체인 미션 로드
            if (config.contains("chains")) {
                ConfigurationSection chains = config.getConfigurationSection("chains");
                for (String chainId : chains.getKeys(false)) {
                    ConfigurationSection chainSection = chains.getConfigurationSection(chainId);
                    loadChainMissions(chainId, chainSection, loadedMissions);
                }
            }
        }

        return loadedMissions;
    }

    /**
     * 체인 내부의 티어별 미션 로드
     */
    private void loadChainMissions(String chainId, ConfigurationSection section,
            Map<String, MissionTemplate> missions) {
        // 체인 공통 설정 (아이콘, 슬롯 등) - 필요 시 저장
        Material commonIcon = Material.getMaterial(section.getString("icon", "PAPER"));

        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) { // tier_1, tier_2 ...
                // 실제 미션 ID는 "chain_tier" 형태로 저장하여 유니크하게 관리
                String missionId = chainId + "_" + key;
                MissionTemplate mission = loadSingleMission(missionId, section.getConfigurationSection(key));

                if (mission != null) {
                    mission.setChainId(chainId);
                    if (mission.getIcon() == null) {
                        mission.setIcon(commonIcon);
                    }
                    missions.put(missionId, mission);
                }
            }
        }
    }

    /**
     * 단일 미션 템플릿 파싱
     */
    private MissionTemplate loadSingleMission(String id, ConfigurationSection section) {
        try {
            MissionTemplate template = new MissionTemplate();
            template.setId(id);
            template.setDisplayName(translateColors(section.getString("display_name", id)));
            template.setDescription(translateColors(section.getStringList("lore"))); // YAML에서 description 대신 lore 많이 씀

            // 타입
            String typeStr = section.getString("type", "BREAK").toUpperCase();
            try {
                template.setType(MissionType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                plugin.log(Level.WARNING, "알 수 없는 미션 타입: " + typeStr + " (ID: " + id + ")");
                template.setType(MissionType.BREAK);
            }

            // 타겟 파싱 (문자열 또는 리스트)
            if (section.isList("target")) {
                template.setTargets(section.getStringList("target"));
            } else if (section.isString("target")) {
                List<String> targets = new ArrayList<>();
                targets.add(section.getString("target"));
                template.setTargets(targets);
            }

            // 조건 파싱
            if (section.isList("condition")) {
                template.setConditions(section.getStringList("condition"));
            } else if (section.isList("conditions")) {
                template.setConditions(section.getStringList("conditions"));
            }

            // 미션 목표량
            template.setAmount(section.getInt("amount", 1));

            // 아이콘
            String iconStr = section.getString("icon");
            if (iconStr != null) {
                template.setIcon(Material.getMaterial(iconStr.toUpperCase()));
            }

            // 보상 파싱
            ConfigurationSection rewards = section.getConfigurationSection("rewards");
            if (rewards != null) {
                template.setRewardMoney(rewards.getDouble("money", 0));
                template.setRewardItems(rewards.getStringList("items")); // 간편 목록

                // 직업 경험치
                if (rewards.contains("job_exp")) {
                    ConfigurationSection expSec = rewards.getConfigurationSection("job_exp");
                    Map<String, Double> expMap = new HashMap<>();
                    for (String job : expSec.getKeys(false)) {
                        expMap.put(job, expSec.getDouble(job));
                    }
                    template.setRewardJobExp(expMap);
                }

                // Complex Rewards
                template.setRewardCommands(rewards.getStringList("commands"));
                template.setRewardPermissions(rewards.getStringList("permissions"));
                template.setRewardBuffs(rewards.getStringList("buffs"));
                template.setRewardTitle(translateColors(rewards.getString("title")));
                template.setRewardSubtitle(translateColors(rewards.getString("subtitle")));
            }

            // 리스트 형태의 rewards (commands 등 혼합) 지원
            if (section.isList("rewards")) {
                List<String> rawRewards = section.getStringList("rewards");
                List<String> cmds = new ArrayList<>();
                for (String r : rawRewards) {
                    if (r.startsWith("money ")) { // "money give %player% 200"
                        // 간단한 파싱 시도 또는 그대로 커맨드로
                        cmds.add(r.replace("money give %player% ", "dw money give %player% "));
                    } else {
                        cmds.add(r);
                    }
                }
                template.setRewardCommands(cmds);
            }

            // 다음 미션
            String next = section.getString("next_mission");
            if (next != null) {
                // 같은 체인 내의 다음 티어라고 가정하고 ID 조합
                // 하지만 next_mission에 "tier_2"라고 적혀있으면 "chainId_tier_2"로 변환 필요
                // 여기서는 일단 raw string 저장하고 Manager에서 처리하도록 함
                template.setNextMission(next);
            }

            template.setResetCycle(section.getString("reset_cycle", "ONE_TIME"));

            return template;

        } catch (Exception e) {
            plugin.log(Level.WARNING, "미션 로드 중 오류 발생 (" + id + "): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String translateColors(String text) {
        if (text == null)
            return null;
        return text.replace("&", "§");
    }

    private List<String> translateColors(List<String> list) {
        if (list == null)
            return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String s : list) {
            result.add(translateColors(s));
        }
        return result;
    }
}
