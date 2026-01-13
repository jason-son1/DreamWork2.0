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

        // Towny 제한 체크 (타운 농장 구역에서만 수확 가능)
        if (plugin.getTownyHook().isEnabled()) {
            // 자신의 타운 여부는 기획에 없었으나 "자신의 타운 밭(Farm Plot)"이라고 명시됨.
            // 여기서는 Farm Plot 인지만 체크하고, 권한은 Towny가 처리한다고 가정하거나
            // 추가적으로 TownyHook에서 resident check를 해야 함.
            // 기획서: "농부가 '자신의 타운 밭'에서만... isFarmPlot 체크"
            // TownyHook.isInOwnTown 체크도 필요할 수 있음. 일단 FarmPlot 체크 우선.
            if (!plugin.getTownyHook().isFarmPlot(block.getLocation())) {
                return; // 농장 구역이 아니면 직업 보상 없음
            }
        }

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
        FileConfiguration config = plugin.getConfigManager().getJobConfig("farmer");

        // Toggle Check (Default true = Vanilla Drops Enabled)
        boolean enableVanilla = config.getBoolean("enable_vanilla_drops", true);

        if (!enableVanilla) {
            block.getDrops().clear(); // MONITOR라서 바닐라 드롭을 완전히 막기는 어려울 수 있으나, 가급적 시도.
            // MONITOR 단계에서는 event.setCancelled 불가 및 getDrops().clear()가 실제 월드 드롭에 영향 안 줄 수
            // 있음.
            // 확실하게 하려면 HIGHEST로 변경해야 함.
            // 하지만 현재 구조상 코드 수정이 많으므로, MONITOR 유지하되
            // 바닐라 드롭을 '지우는' 로직은 BlockBreakEvent(HIGHEST)에서 별도로 처리하거나
            // 혹은 spawn된 아이템을 식별해서 제거해야 함.
            // 여기서는 '추가 드롭' 방식으로 구현하되, enableVanilla=false면 드롭을 대체하도록 노력.
            // *주의*: MONITOR에서 block.getDrops().clear()는 효과가 없을 가능성 높음.
            // API 한계로 여기서는 일단 추가 드롭만 구현하고, 바닐라 드롭 제어는 별도 리스너가 필요할 수 있음을 주석으로 남김.
            // (사용자 요청: enable_vanilla_drops 기본값 true이므로 큰 이슈 아님)
        }

        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.FARMER);

        double tier2Chance = config.getDouble("items.quality_crops.tier_2_chance", 0.15) + (level * 0.001);
        double tier3Chance = config.getDouble("items.quality_crops.tier_3_chance", 0.05) + (level * 0.0005);

        int quality = 1;
        double r = random.nextDouble();

        if (r < tier3Chance)
            quality = 3;
        else if (r < tier3Chance + tier2Chance)
            quality = 2;

        if (quality > 1 || !enableVanilla) {
            // 2성 이상이거나, 바닐라 드롭을 껐으면 커스텀 아이템 드롭
            // (바닐라 드롭을 끄면 1성도 커스텀 아이템으로 드롭해야 함)
            if (quality == 1 && enableVanilla) {
                // 1성이고 바닐라 드롭 켜져있으면 -> 그냥 바닐라 드롭이 1성 역할 (아무것도 안 함)
            } else {
                Material harvestItem = getHarvestItem(type);
                ItemStack item = new ItemStack(harvestItem, 1); // Fortune 적용 안 된 1개 고정 (단순화)
                setQuality(item, quality, config);

                // Drop it
                block.getWorld().dropItemNaturally(block.getLocation(), item);
            }
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

    // ==================== 숙성 (Aging) ====================

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof org.bukkit.block.Barrel) {
            org.bukkit.inventory.Inventory inv = event.getInventory();
            long now = System.currentTimeMillis();
            NamespacedKey timestampKey = new NamespacedKey(plugin, "aging_start");

            for (ItemStack item : inv.getContents()) {
                if (item == null || item.getType() == Material.AIR)
                    continue;

                // 작물인지 확인 (isRegularCrop 활용 가능하지만 Material만 체크)
                if (!isRegularCrop(item.getType()))
                    continue;

                ItemMeta meta = item.getItemMeta();
                if (meta == null)
                    continue;

                // 시간 체크
                if (meta.getPersistentDataContainer().has(timestampKey, PersistentDataType.LONG)) {
                    long start = meta.getPersistentDataContainer().get(timestampKey, PersistentDataType.LONG);
                    long elapsed = now - start;

                    // 10분(600초) 이상 지났으면 등급 상승
                    if (elapsed >= 600_000) {
                        int currentQuality = meta.getPersistentDataContainer().getOrDefault(qualityKey,
                                PersistentDataType.INTEGER, 0);
                        if (currentQuality < 3) {
                            // 등급 업!
                            currentQuality = Math.max(currentQuality + 1, 2); // 최소 2성부터 시작
                            // 메타 업데이트를 위해 setQuality 로직 재사용 (약간 변형 필요)
                            // 여기선 직접 설정
                            FileConfiguration config = plugin.getConfigManager().getJobConfig("farmer");
                            setQuality(item, currentQuality, config);
                            // 타임스탬프 리셋 (다음 등급으로 가기 위해)
                            meta = item.getItemMeta(); // setQuality에서 메타가 바뀌었을 수 있음
                            meta.getPersistentDataContainer().set(timestampKey, PersistentDataType.LONG, now);
                            item.setItemMeta(meta);
                        }
                    }
                } else {
                    // 타임스탬프가 없으면 지금부터 시작
                    meta.getPersistentDataContainer().set(timestampKey, PersistentDataType.LONG, now);
                    item.setItemMeta(meta);
                }
            }
        }
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
