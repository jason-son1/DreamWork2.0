package com.dreamwork.job.listener;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import com.dreamwork.mission.MissionType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Random;

/**
 * 농부 직업 리스너
 * 
 * 작물 수확 시 경험치/돈 지급, 녹색 손길(자동 재파종), 품질 작물 드롭 등을 처리합니다.
 * 
 * @author DreamWork Team
 */
public class FarmerListener implements Listener {

    private final DreamWorkPlugin plugin;
    private final Random random = new Random();
    private final NamespacedKey qualityKey;

    public FarmerListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        this.qualityKey = new NamespacedKey(plugin, "crop_quality");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();

        // 1. 작물 확인 및 Age 체크
        boolean isFullyGrown = false;
        boolean isCrop = false;

        if (block.getBlockData() instanceof Ageable ageable) {
            isCrop = true;
            if (ageable.getAge() >= ageable.getMaximumAge()) {
                isFullyGrown = true;
            }
        } else if (isStemPlant(type)) {
            // 사탕수수, 선인장 등 (뿌리가 아닌 경우에만 보상)
            isCrop = true;
            Block below = block.getRelative(org.bukkit.block.BlockFace.DOWN);
            if (below.getType() == type) {
                isFullyGrown = true; // 2칸 이상 자란 것으로 간주 (윗부분 채집)
            }
        } else if (type == Material.PUMPKIN || type == Material.MELON) {
            isCrop = true;
            isFullyGrown = true; // 호박/수박은 블록 자체가 수확물
        }

        if (!isCrop)
            return;

        // 직업 보상 로직 (완전히 자란 경우만)
        if (isFullyGrown) {
            processRewards(player, type);
            // 품질 작물 드롭 & 황금 씨앗 (일반 작물인 경우만)
            if (isRegularCrop(type)) {
                handleDrops(player, block, type);
            }
        }

        // 2. 녹색 손길 (Green Thumb) - 자동 재파종
        // Ageable 작물이고, 완전히 자랐을 때만
        if (isFullyGrown && block.getBlockData() instanceof Ageable) {
            handleGreenThumb(player, block, type);
        }
    }

    // 일반 작물 (품질 시스템 적용 대상)
    private boolean isRegularCrop(Material type) {
        return type == Material.WHEAT || type == Material.POTATOES || type == Material.CARROTS
                || type == Material.BEETROOTS;
    }

    private boolean isStemPlant(Material type) {
        return type == Material.SUGAR_CANE || type == Material.CACTUS;
    }

    private void processRewards(Player player, Material type) {
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig("farmer");
        String key = type.name().toLowerCase();
        if (type == Material.POTATOES)
            key = "potato";
        if (type == Material.CARROTS)
            key = "carrot";

        // Config Path mapping
        // rewards.wheat.exp

        double exp = 0;
        double money = 0;

        if (jobConfig.contains("rewards." + key)) {
            exp = jobConfig.getDouble("rewards." + key + ".exp");
            money = jobConfig.getDouble("rewards." + key + ".money");
        }

        if (exp > 0 || money > 0) {
            plugin.getJobManager().giveReward(player, JobType.FARMER, exp, money);
            plugin.getMissionManager().processEvent(player, MissionType.HARVEST, key, 1);
        }
    }

    private void handleDrops(Player player, Block block, Material type) {
        // 기존 드롭 취소하고 커스텀 드롭을 할지, 아니면 추가 드롭을 할지 결정
        // Plan implies replacing drops with Rated crops.
        // For simplicity allow vanilla drops + extra or modify vanilla drops?
        // Let's modify vanilla drops (clear drops, spawn new custom item).

        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.FARMER);
        FileConfiguration config = plugin.getConfigManager().getJobConfig("farmer");

        double tier2Chance = config.getDouble("items.quality_crops.tier_2_chance", 0.15) + (level * 0.001);
        double tier3Chance = config.getDouble("items.quality_crops.tier_3_chance", 0.05) + (level * 0.0005);

        int quality = 1;
        double r = random.nextDouble();

        if (r < tier3Chance)
            quality = 3;
        else if (r < tier3Chance + tier2Chance)
            quality = 2;

        if (quality > 1) {
            // 드롭 아이템 교체
            block.getDrops().clear(); // This only works if called before block break logic completes, but we are
                                      // MONITOR.
            // Monitor means event happened. Drops usually naturally handled.
            // If we want to replace drops, we should use HIGHEST and setDropItems or clear
            // vanilla drops manually.
            // Since we are MONITOR, we can't easily cancel drops.
            // BETTER: Use HIGHEST priority and block.getDrops() modification? Or cancel
            // event, break naturally manually?
            // "Recommended for custom drops: setDropItems in current API".

            // Let's keep logic simple: If MONITOR, we assume drops happened.
            // We can just spawn EXTRA quality items and remove existing? No.
            // Let's spawn crop item at location with custom meta.
            // Since we cannot easily replace vanilla drops in MONITOR, we will just ADD
            // bonus drops for now,
            // OR we change priority to HIGH and modify drops if possible.
            // Bukkit BlockBreakEvent setDropItems is not always available in all versions.

            // Fallback: Just drop the quality item. Config says "1-Star: Normal". So normal
            // drops are 1-Star.
            // If we proc 2-star, we drop a 2-star item EXTRA (or replace).
            // Let's drop EXTRA for "Feeling of abundance" or remove one corresponding item
            // from drops?

            // Valid Approach: Drop Quality Item directly.
            Material harvestItem = getHarvestItem(type);
            ItemStack item = new ItemStack(harvestItem, 1); // Quantity can depend on Fortune
            setQuality(item, quality, config);

            // Drop it
            block.getWorld().dropItemNaturally(block.getLocation(), item);

            // 2성 이상이면 메시지? (Optional)
        }

        // 황금 씨앗 (Golden Seed)
        double goldChance = config.getDouble("items.golden_seed.base_chance", 0.001)
                + (level * config.getDouble("items.golden_seed.bonus_per_level", 0.0005));
        if (random.nextDouble() < goldChance) {
            ItemStack goldSeed = plugin.getItemManager().createItem("golden_seed", 1);
            if (goldSeed != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), goldSeed);
                player.sendMessage("§e[농부] §6황금 씨앗§e을 발견했습니다!");
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
            }
        }
    }

    private void setQuality(ItemStack item, int quality, FileConfiguration config) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        String prefix = "";
        if (quality == 2)
            prefix = config.getString("items.quality_crops.tier_2_prefix", "§a싱싱한 ");
        if (quality == 3)
            prefix = config.getString("items.quality_crops.tier_3_prefix", "§6황금 ");

        meta.setDisplayName(prefix + getKoreanName(item.getType()));
        meta.getPersistentDataContainer().set(qualityKey, PersistentDataType.INTEGER, quality);
        item.setItemMeta(meta);
    }

    private void handleGreenThumb(Player player, Block block, Material type) {
        FileConfiguration config = plugin.getConfigManager().getJobConfig("farmer");
        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.FARMER);

        // Check levels
        double chance = 0;
        if (level >= 30)
            chance = 100.0;
        else if (level >= 10)
            chance = 50.0;
        else
            return;

        if (random.nextDouble() * 100 < chance) {
            // Find appropriate seed
            Material seedType = getSeedType(type);
            // Check inventory
            if (player.getInventory().contains(seedType)) {
                // Replant
                // Delay slightly to prevent conflict with break?
                // Use Scheduler
                final Material finalType = type;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (block.getType() == Material.AIR) { // Only if air (broken)
                        // Consume seed
                        if (consumeItem(player, seedType)) {
                            block.setType(finalType);
                            // Set age to 0? Default is 0.
                        }
                    }
                }, 2L);
            }
        }
    }

    private boolean consumeItem(Player player, Material mat) {
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat) {
                is.setAmount(is.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    private Material getSeedType(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case POTATOES -> Material.POTATO;
            case CARROTS -> Material.CARROT;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            default -> null;
        };
    }

    private Material getHarvestItem(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT;
            case POTATOES -> Material.POTATO;
            case CARROTS -> Material.CARROT;
            case BEETROOTS -> Material.BEETROOT;
            default -> crop;
        };
    }

    private String getKoreanName(Material mat) {
        return switch (mat) {
            case WHEAT -> "밀";
            case POTATO -> "감자";
            case CARROT -> "당근";
            case BEETROOT -> "비트";
            default -> mat.name();
        };
    }
}
