package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Random;

/**
 * 녹색 손길 스킬 (농부)
 * 
 * 수확 시 일정 확률로 작물을 자동 재파종합니다.
 * 패시브 스킬로, 해금 후 자동 적용됩니다.
 * 
 * 해금 레벨: 10
 * 기본 확률: 30%
 * 
 * @author DreamWork Team
 */
public class GreenThumb implements Listener {

    private final DreamWorkPlugin plugin;
    private final Random random = new Random();
    private static final String SKILL_ID = "green_thumb";

    public GreenThumb(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 블록 파괴 이벤트 - 자동 재파종 처리
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        // 작물인지 확인
        if (!isCrop(block.getType()))
            return;

        // 완전히 성장한 작물인지 확인
        if (!isFullyGrown(block))
            return;

        // 스킬 해금 확인
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int level = userData.getJobLevel(JobType.FARMER);
        int unlockLevel = plugin.getSkillManager().getSkillUnlockLevel(JobType.FARMER, SKILL_ID);

        if (level < unlockLevel)
            return;

        // 확률 확인
        double baseChance = plugin.getConfigManager().getJobConfig("farmer")
                .getDouble("skills.green_thumb.chance", 0.3);

        // 레벨 보너스 (레벨당 0.5% 추가)
        double levelBonus = (level - unlockLevel) * 0.005;
        double finalChance = Math.min(0.8, baseChance + levelBonus); // 최대 80%

        if (random.nextDouble() > finalChance)
            return;

        // 자동 재파종
        Material seedMaterial = getSeedMaterial(block.getType());
        if (seedMaterial == null)
            return;

        // 1틱 후 재파종 (블록 파괴 후)
        Block farmland = block.getRelative(BlockFace.DOWN);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // 경작지 확인
            if (farmland.getType() == Material.FARMLAND) {
                block.setType(seedMaterial);

                // 파티클 효과
                player.spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        block.getLocation().add(0.5, 0.5, 0.5),
                        5,
                        0.3, 0.3, 0.3,
                        0);

                // 사운드
                player.playSound(block.getLocation(), Sound.ITEM_CROP_PLANT, 0.5f, 1.2f);
            }
        }, 1L);
    }

    /**
     * 작물인지 확인
     */
    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS -> true;
            default -> false;
        };
    }

    /**
     * 완전히 성장한 작물인지 확인
     */
    private boolean isFullyGrown(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    /**
     * 작물에 해당하는 씨앗 Material
     */
    private Material getSeedMaterial(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT;
            case CARROTS -> Material.CARROTS;
            case POTATOES -> Material.POTATOES;
            case BEETROOTS -> Material.BEETROOTS;
            default -> null;
        };
    }
}
