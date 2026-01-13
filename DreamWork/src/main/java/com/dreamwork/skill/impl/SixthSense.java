package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StructureSearchResult;

/**
 * 육감 (Sixth Sense) - 모험가 패시브
 * 주기적으로 주변 구조물을 감지하여 힌트를 제공합니다.
 */
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
            if (level < 20)
                continue;

            int range = 30; // Lv 20
            if (level >= 50)
                range = 50; // Lv 50

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
        // List of structures to check
        Structure[] targets;
        if (level >= 50) {
            targets = new Structure[] {
                    Structure.ANCIENT_CITY, Structure.STRONGHOLD, Structure.MINESHAFT,
                    Structure.DESERT_PYRAMID, Structure.JUNGLE_PYRAMID
            };
        } else {
            targets = new Structure[] {
                    Structure.MINESHAFT, Structure.PILLAGER_OUTPOST
            };
        }

        double minDst = Double.MAX_VALUE;
        String foundName = null;

        for (Structure type : targets) {
            try {
                StructureSearchResult result = loc.getWorld().locateNearestStructure(loc, type, range, false);
                if (result != null) {
                    double dst = result.getLocation().distance(loc);
                    if (dst <= range && dst < minDst) {
                        minDst = dst;
                        // Use key().getKey() if available, else toString fallback
                        foundName = type.getKey().getKey(); // "ancient_city", "mineshaft"
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (foundName != null) {
            player.sendMessage("§5[육감] §d주변에서 " + foundName + "의 기운이 느껴집니다... (" + (int) minDst + "m)");
            player.playSound(loc, org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.5f);
        }
    }
}
