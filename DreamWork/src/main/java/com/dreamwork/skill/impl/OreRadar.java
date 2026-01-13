package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import com.dreamwork.skill.SkillManager;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
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

        // 유저 레벨 확인
        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.MINER);
        FileConfiguration config = plugin.getConfigManager().getJobConfig("miner");

        // 설정 가져오기 (레벨별)
        int range = 10;
        int cooldown = 60;
        String detailLevel = "existence";

        if (level >= 50) {
            range = config.getInt("skills.ore_radar.levels.50.radius", 30);
            cooldown = config.getInt("skills.ore_radar.levels.50.cooldown", 15);
            detailLevel = "rare";
        } else if (level >= 30) {
            range = config.getInt("skills.ore_radar.levels.30.radius", 30);
            cooldown = config.getInt("skills.ore_radar.levels.30.cooldown", 30);
            detailLevel = "exact";
        } else if (level >= 10) {
            range = config.getInt("skills.ore_radar.levels.10.radius", 20);
            cooldown = config.getInt("skills.ore_radar.levels.10.cooldown", 45);
            detailLevel = "rough";
        } else {
            range = config.getInt("skills.ore_radar.levels.1.radius", 10);
            cooldown = config.getInt("skills.ore_radar.levels.1.cooldown", 60);
        }

        // 주변 광물 탐지
        List<Block> ores = findNearbyOres(player, range);

        if (ores.isEmpty()) {
            player.sendMessage("§7주변에서 광물을 발견하지 못했습니다.");
        } else {
            sendDetailMessage(player, ores, detailLevel);
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
     * 상세도에 따른 메시지 출력
     */
    private void sendDetailMessage(Player player, List<Block> ores, String detailLevel) {
        switch (detailLevel) {
            case "existence":
                player.sendMessage("§a✦ 광맥 탐지! §f근처에서 광물이 감지되었습니다.");
                break;
            case "rough":
                int count = ores.size();
                String amountMsg = count > 20 ? "다량의" : (count > 5 ? "소량의" : "약간의");
                player.sendMessage("§a✦ 광맥 탐지! §f근처에서 " + amountMsg + " 광물이 감지되었습니다.");
                break;
            case "exact":
            case "rare":
                player.sendMessage("§a✦ 광맥 탐지! §f총 " + ores.size() + "개의 광물을 발견했습니다.");
                // 가장 가까운 광물 정보 (최대 3개)
                ores.sort((b1, b2) -> Double.compare(
                        b1.getLocation().distanceSquared(player.getLocation()),
                        b2.getLocation().distanceSquared(player.getLocation())));

                for (int i = 0; i < Math.min(3, ores.size()); i++) {
                    Block b = ores.get(i);
                    double dist = Math.sqrt(b.getLocation().distanceSquared(player.getLocation()));
                    String direction = getDirection(player.getLocation(), b.getLocation());
                    String name = b.getType().name().replace("_ORE", "").replace("DEEPSLATE_", "").toLowerCase();
                    player.sendMessage("  §7- " + direction + " " + String.format("%.1f", dist) + "m: §e" + name);
                }

                if (detailLevel.equals("rare")) {
                    // 희귀 광물(다이아, 고대 잔해 등) 강조
                    long rareCount = ores.stream()
                            .filter(b -> b.getType() == Material.DIAMOND_ORE
                                    || b.getType() == Material.DEEPSLATE_DIAMOND_ORE ||
                                    b.getType() == Material.ANCIENT_DEBRIS || b.getType() == Material.EMERALD_ORE ||
                                    b.getType() == Material.DEEPSLATE_EMERALD_ORE)
                            .count();
                    if (rareCount > 0) {
                        player.sendMessage("§d✨ 특별한 기운이 느껴집니다! (희귀 광물 " + rareCount + "개 감지)");
                    }
                }
                break;
        }
    }

    private String getDirection(Location from, Location to) {
        double rot = Math.toDegrees(Math.atan2(from.getX() - to.getX(), to.getZ() - from.getZ()));
        if (rot < 0)
            rot += 360;

        if (rot >= 315 || rot < 45)
            return "북쪽";
        if (rot >= 45 && rot < 135)
            return "동쪽";
        if (rot >= 135 && rot < 225)
            return "남쪽";
        return "서쪽";
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
                    COAL_ORE, DEEPSLATE_COAL_ORE,
                    IRON_ORE, DEEPSLATE_IRON_ORE,
                    COPPER_ORE, DEEPSLATE_COPPER_ORE,
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

                if (!player.isOnline()) {
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
                            0.1, 0.1, 0.1,
                            0.05,
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
