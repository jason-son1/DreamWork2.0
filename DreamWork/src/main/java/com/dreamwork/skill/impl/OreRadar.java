package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import com.dreamwork.skill.SkillManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * 광맥 탐지 스킬 (광부)
 * 
 * 주변의 귀중한 광물을 탐지하여 파티클로 표시합니다.
 * 
 * 해금 레벨: 10
 * 쿨다운: 60초
 * 범위: 16블록
 * 
 * @author DreamWork Team
 */
public class OreRadar {

    private final DreamWorkPlugin plugin;
    private static final String SKILL_ID = "ore_radar";

    public OreRadar(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 스킬 사용
     */
    public boolean use(Player player) {
        SkillManager skillManager = plugin.getSkillManager();

        // 사용 가능 여부 확인
        if (!skillManager.canUseSkill(player, JobType.MINER, SKILL_ID)) {
            return false;
        }

        // 스킬 설정 가져오기
        int range = plugin.getConfigManager().getJobConfig("miner")
                .getInt("skills.ore_radar.range", 16);
        int cooldown = skillManager.getSkillCooldown(JobType.MINER, SKILL_ID);

        // 주변 광물 탐지
        List<Block> ores = findNearbyOres(player, range);

        if (ores.isEmpty()) {
            player.sendMessage("§7주변에서 광물을 발견하지 못했습니다.");
        } else {
            player.sendMessage("§a✦ 광맥 탐지! §f" + ores.size() + "개의 광물을 발견했습니다.");

            // 파티클 효과 시작
            showOreParticles(player, ores);
        }

        // 사운드 재생
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        // 쿨다운 시작
        skillManager.startCooldown(player, SKILL_ID, cooldown);

        return true;
    }

    /**
     * 주변 광물 찾기
     */
    private List<Block> findNearbyOres(Player player, int range) {
        List<Block> ores = new ArrayList<>();
        Location center = player.getLocation();
        World world = player.getWorld();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    Block block = world.getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z);

                    if (isValuableOre(block.getType())) {
                        ores.add(block);
                    }
                }
            }
        }

        return ores;
    }

    /**
     * 귀중한 광물인지 확인
     */
    private boolean isValuableOre(Material material) {
        return switch (material) {
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                    EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                    GOLD_ORE, DEEPSLATE_GOLD_ORE,
                    LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                    REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                    ANCIENT_DEBRIS ->
                true;
            default -> false;
        };
    }

    /**
     * 광물 위치에 파티클 표시 (5초간)
     */
    private void showOreParticles(Player player, List<Block> ores) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100) { // 5초 (100틱)
                    cancel();
                    return;
                }

                for (Block ore : ores) {
                    Location loc = ore.getLocation().add(0.5, 0.5, 0.5);

                    // 광물 종류별 색상
                    Particle.DustOptions dust = getOreParticleColor(ore.getType());

                    player.spawnParticle(
                            Particle.DUST,
                            loc,
                            3,
                            0.3, 0.3, 0.3,
                            0,
                            dust);
                }

                ticks += 10;
            }
        }.runTaskTimer(plugin, 0, 10);
    }

    /**
     * 광물 종류별 파티클 색상
     */
    private Particle.DustOptions getOreParticleColor(Material material) {
        Color color = switch (material) {
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> Color.AQUA;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> Color.GREEN;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> Color.YELLOW;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> Color.BLUE;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> Color.RED;
            case ANCIENT_DEBRIS -> Color.fromRGB(139, 69, 19);
            default -> Color.WHITE;
        };

        return new Particle.DustOptions(color, 1.5f);
    }
}
