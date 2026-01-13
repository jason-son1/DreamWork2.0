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

            // Optimization: Skip check if player hasn't moved much
            if (hasMovedSignificantly(player)) {
                // 비동기로 무거운 작업 실행
                final int finalRange = range;
                final int finalLevel = level;
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    checkForStructure(player, finalRange, finalLevel);
                });
            }
        }
    }

    private final java.util.Map<java.util.UUID, Location> lastLocations = new java.util.HashMap<>();

    private boolean hasMovedSignificantly(Player player) {
        Location current = player.getLocation();
        Location last = lastLocations.get(player.getUniqueId());

        // 16블록(1청크) 이상 이동했을 때만 체크
        if (last == null || !last.getWorld().equals(current.getWorld()) || last.distanceSquared(current) > 256) {
            lastLocations.put(player.getUniqueId(), current);
            return true;
        }
        return false;
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
                    Structure.VILLAGE_PLAINS,
                    Structure.VILLAGE_DESERT,
                    Structure.VILLAGE_SAVANNA,
                    Structure.VILLAGE_TAIGA,
                    Structure.VILLAGE_SNOWY,
                    Structure.SHIPWRECK,
                    Structure.BURIED_TREASURE
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
            final String finalName = foundName;
            final double finalDst = minDst;

            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage("§5[육감] §d주변에서 " + finalName + "의 기운이 느껴집니다... (" + (int) finalDst + "m)");
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.5f);

                    // 시각적 효과 (Lv 50+)
                    if (level >= 50 && finalDst < 15) {
                        player.spawnParticle(org.bukkit.Particle.DRAGON_BREATH, player.getLocation().add(0, 1, 0), 10,
                                0.5, 0.5, 0.5, 0.05);
                    }
                }
            });
        }
    }
}
