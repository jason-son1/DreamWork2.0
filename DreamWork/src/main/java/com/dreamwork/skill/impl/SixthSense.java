package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.StructureSearchResult;

/**
 * 육감 (Sixth Sense) - 모험가 패시브
 * 주기적으로 주변 구조물을 감지하여 힌트를 제공합니다.
 */
@SuppressWarnings("deprecation")
public class SixthSense extends BukkitRunnable {

    private final DreamWorkPlugin plugin;

    public SixthSense(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.isEnabled()) {
            cancel();
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.ADVENTURER);
            if (level < 1)
                continue;

            // Discovery works always, Hint works at Lv 20+
            int range = 50; // Discovery Range (fixed)
            if (level >= 20)
                range = 100; // Hint Range
            if (level >= 50)
                range = 200;

            // Structure Scan
            // Warning: locateNearestStructure is main thread heavy if not careful.
            // But we are in run() which is main thread? No, we should run this async if
            // possible or use async method.
            // But Bukkit API locate is main thread usually?
            // Actually locateNearestStructure might be heavy.
            // Optimization: Only run if player moved significantly? Or very long interval
            // (60s).
            // Plugin uses 1200 ticks (60s) interval.

            checkForStructure(player, range, level);
        }
    }

    private void checkForStructure(Player player, int range, int level) {
        Location loc = player.getLocation();

        Structure[] targets;

        if (level >= 50) {
            targets = new Structure[] {
                    Structure.ANCIENT_CITY,
                    Structure.STRONGHOLD,
                    Structure.MINESHAFT,
                    Structure.DESERT_PYRAMID,
                    Structure.JUNGLE_PYRAMID,
                    Structure.MONUMENT
            };
        } else {
            targets = new Structure[] {
                    Structure.MINESHAFT,
                    Structure.PILLAGER_OUTPOST,
                    // Legacy VILLAGE covered all types; listing common variants for compatibility
                    Structure.VILLAGE_PLAINS,
                    Structure.VILLAGE_DESERT,
                    Structure.VILLAGE_SAVANNA,
                    Structure.VILLAGE_TAIGA,
                    Structure.VILLAGE_SNOWY
            };
        }

        double minDst = Double.MAX_VALUE;
        String foundName = null;

        for (Structure type : targets) {
            try {
                // locateNearestStructure usage with Structure (returns StructureSearchResult in
                // 1.21+)
                StructureSearchResult result = loc.getWorld().locateNearestStructure(loc, type, range, false);
                if (result != null) {
                    Location resultLoc = result.getLocation();
                    double dst = resultLoc.distance(loc);
                    String name = type.getKey().getKey();

                    // 발견 미션 처리 (거리 50m 이내)
                    if (dst < 50) {
                        String missionKey = name.toUpperCase();
                        plugin.getMissionManager().processEvent(player,
                                com.dreamwork.mission.MissionType.DISCOVER_STRUCTURE, missionKey, 1);
                        plugin.getMissionManager().processEvent(player,
                                com.dreamwork.mission.MissionType.DISCOVER_STRUCTURE, "ANY", 1);
                    }

                    if (dst <= range && dst < minDst) {
                        minDst = dst;
                        foundName = name; // e.g. "ancient_city"
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 육감 힌트 (Lv 20+)
        if (foundName != null && level >= 20) {
            player.sendMessage("§5[육감] §d주변에서 " + foundName + "의 기운이 느껴집니다... (" + (int) minDst + "m)");
            player.playSound(loc, org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.5f);

            // 시각적 효과 (Lv 50+)
            if (level >= 50 && minDst < 15) {
                player.spawnParticle(org.bukkit.Particle.DRAGON_BREATH, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.05);
            }
        }
    }
}
