package com.dreamwork.job.listener;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import com.dreamwork.mission.MissionType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * 농부 직업 리스너
 * 
 * 작물 수확 시 경험치와 돈을 지급합니다.
 * 완전히 성장한 작물만 보상 대상입니다.
 * 
 * @author DreamWork Team
 */
public class FarmerListener implements Listener {

    private final DreamWorkPlugin plugin;
    private final Random random = new Random();

    public FarmerListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 블록 파괴 이벤트 - 작물 수확 처리
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();

        // 작물인지 확인
        if (!isCrop(material))
            return;

        // 완전히 성장한 작물인지 확인
        if (!isFullyGrown(block))
            return;

        // 직업 설정 가져오기
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig("farmer");
        if (jobConfig == null)
            return;

        // 작물별 보상 확인
        ConfigurationSection cropsSection = jobConfig.getConfigurationSection("crops");
        if (cropsSection == null)
            return;

        String cropKey = getCropKey(material);
        ConfigurationSection cropConfig = cropsSection.getConfigurationSection(cropKey);

        if (cropConfig == null)
            return;

        // 경험치 및 돈 가져오기
        double exp = cropConfig.getDouble("exp", 0);
        double money = cropConfig.getDouble("money", 0);

        if (exp <= 0 && money <= 0)
            return;

        // 쿨다운 체크
        var userData = plugin.getUserDataManager().getUserData(player);
        long cooldown = plugin.getConfigManager().getConfig()
                .getLong("anti-abuse.action-cooldown", 500);

        if (!userData.checkAndUpdateCooldown("farm_" + cropKey, cooldown)) {
            return;
        }

        // 보상 지급
        plugin.getJobManager().giveReward(player, JobType.FARMER, exp, money);

        // 미션 이벤트 트리거
        plugin.getMissionManager().processEvent(player, MissionType.BREAK, cropKey, 1);

        // 등급별 작물 드롭 처리
        handleQualityCropDrop(player, cropKey);

        plugin.debug(player.getName() + " 수확: " + cropKey +
                " (Exp: " + exp + ", Money: " + money + ")");
    }

    /**
     * 작물 Material인지 확인
     */
    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS,
                    NETHER_WART, COCOA, MELON, PUMPKIN,
                    SWEET_BERRY_BUSH ->
                true;
            default -> false;
        };
    }

    /**
     * 완전히 성장한 작물인지 확인
     */
    private boolean isFullyGrown(Block block) {
        Material material = block.getType();

        // 멜론/호박은 항상 성장 완료
        if (material == Material.MELON || material == Material.PUMPKIN) {
            return true;
        }

        // Ageable 블록 확인
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }

        return true;
    }

    /**
     * Material을 설정 파일 키로 변환
     */
    private String getCropKey(Material material) {
        return switch (material) {
            case WHEAT -> "wheat";
            case CARROTS -> "carrot";
            case POTATOES -> "potato";
            case BEETROOTS -> "beetroot";
            case NETHER_WART -> "nether_wart";
            case COCOA -> "cocoa";
            case MELON -> "melon";
            case PUMPKIN -> "pumpkin";
            case SWEET_BERRY_BUSH -> "sweet_berry";
            default -> material.name().toLowerCase();
        };
    }

    /**
     * 등급별 작물 드롭 처리
     * 레벨이 높을수록 고등급 작물 확률 증가
     */
    private void handleQualityCropDrop(Player player, String cropKey) {
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig("farmer");
        if (jobConfig == null)
            return;

        // 등급 확률 가져오기
        double star3Chance = jobConfig.getDouble("quality_chances.star3", 0.05);
        double star2Chance = jobConfig.getDouble("quality_chances.star2", 0.20);

        // 레벨 보너스 (레벨당 0.5% 증가)
        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.FARMER);
        double levelBonus = level * 0.005;

        double roll = random.nextDouble();

        // 3성 작물
        if (roll < star3Chance + levelBonus) {
            ItemStack qualityCrop = plugin.getItemManager()
                    .createItem(cropKey + "_3star", 1);

            if (qualityCrop != null) {
                player.getInventory().addItem(qualityCrop).values()
                        .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

                String message = plugin.getConfigManager()
                        .getMessage("farmer.quality-crop")
                        .replace("{stars}", "3")
                        .replace("{crop}", cropKey);
                player.sendMessage(plugin.getConfigManager().getMessage("prefix") + message);
            }
        }
        // 2성 작물
        else if (roll < star2Chance + (levelBonus * 2)) {
            ItemStack qualityCrop = plugin.getItemManager()
                    .createItem(cropKey + "_2star", 1);

            if (qualityCrop != null) {
                player.getInventory().addItem(qualityCrop).values()
                        .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            }
        }
    }
}
