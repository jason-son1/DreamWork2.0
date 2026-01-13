package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * 대지의 기운 (Farmer Passive)
 * 주기적으로 주변 작물의 성장을 촉진합니다.
 */
public class GrowthAura extends BukkitRunnable {

    private final DreamWorkPlugin plugin;
    private final Random random = new Random();

    public GrowthAura(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.isEnabled()) {
            this.cancel();
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // 직업 및 레벨 체크
            int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.FARMER);
            if (level < 20)
                continue; // Lv 20부터 해금

            // 레벨별 설정
            int radius = 0;
            double chance = 0.0;

            // From Plan:
            // Lv 20: R3, 5%
            // Lv 50: R5, 10%
            // Lv 100(Max): R7, 20%

            if (level >= 100) {
                radius = 7;
                chance = 0.20;
            } else if (level >= 50) {
                radius = 5;
                chance = 0.10;
            } else {
                radius = 3;
                chance = 0.05;
            }

            // 주변 스캔
            Block center = player.getLocation().getBlock();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -1; y <= 1; y++) { // Y 범위는 좁게
                    for (int z = -radius; z <= radius; z++) {
                        Block target = center.getRelative(x, y, z);
                        if (target.getType() == Material.AIR)
                            continue;

                        if (target.getBlockData() instanceof Ageable ageable) {
                            if (ageable.getAge() < ageable.getMaximumAge()) {
                                if (random.nextDouble() < chance) {
                                    ageable.setAge(ageable.getAge() + 1);
                                    target.setBlockData(ageable);
                                    try {
                                        target.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER,
                                                target.getLocation().add(0.5, 0.5, 0.5), 1, 0.2, 0.2, 0.2, 0);
                                    } catch (Exception e) {
                                        // Ignore if particle invalid (version comp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
