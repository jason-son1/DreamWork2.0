package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.generator.structure.Structure;
import org.bukkit.entity.Player;

/**
 * 육감 스킬 (탐험가)
 * 
 * 주변의 구조물(던전, 폐광, 유적 등)을 탐지합니다.
 * 액티브 스킬로 변경됨.
 * 
 * 해금 레벨: 20
 * 쿨다운: 5분
 * 
 * @author DreamWork Team
 */
public class ExplorerSense {

    private final DreamWorkPlugin plugin;
    private static final String SKILL_ID = "sixth_sense";

    public ExplorerSense(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 스킬 사용
     */
    public boolean use(Player player) {
        // 스킬 해금 확인
        if (!plugin.getSkillManager().canUseSkill(player, JobType.ADVENTURER, SKILL_ID)) {
            return false;
        }

        int range = plugin.getConfigManager().getJobConfig("adventurer")
                .getInt("skills.sixth_sense.range", 500);
        int cooldown = plugin.getSkillManager().getSkillUnlockLevel(JobType.ADVENTURER, SKILL_ID) > 0 ? 300 : 0;

        Location loc = player.getLocation();
        Location nearest = null;
        String nearestName = "";
        double minDistance = Double.MAX_VALUE;

        // 탐지할 구조물 목록 (1.21 API)
        Structure[] structures = {
                Structure.MINESHAFT, Structure.STRONGHOLD, Structure.DESERT_PYRAMID,
                Structure.JUNGLE_PYRAMID, Structure.SWAMP_HUT, Structure.IGLOO,
                Structure.VILLAGE_PLAINS, Structure.BURIED_TREASURE, Structure.RUINED_PORTAL
        };

        for (Structure type : structures) {
            try {
                // locateNearestStructure(Location origin, Structure structure, int radius,
                // boolean findUnexplored)
                org.bukkit.util.StructureSearchResult result = loc.getWorld().locateNearestStructure(loc, type, range,
                        false);
                if (result != null) {
                    Location found = result.getLocation();
                    double dist = loc.distanceSquared(found);
                    if (dist < minDistance) {
                        minDistance = dist;
                        nearest = found;
                        // Use getKey() for namespaced key (e.g. minecraft:mineshaft)
                        nearestName = type.getKey().getKey();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (nearest == null) {
            player.sendMessage("§7주변 반경 " + range + "블록 내에 구조물이 없습니다.");
            return false;
        }

        // 나침반 설정
        player.setCompassTarget(nearest);

        player.sendMessage("§d👁 육감 발동! §f" + nearestName + " 발견! (" + (int) Math.sqrt(minDistance) + "m)");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.5f);
        player.spawnParticle(org.bukkit.Particle.WITCH, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);

        plugin.getSkillManager().startCooldown(player, SKILL_ID, cooldown);
        return true;
    }
}
