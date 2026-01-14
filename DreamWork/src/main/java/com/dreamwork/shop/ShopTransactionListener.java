package com.dreamwork.shop;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

/**
 * 상점 거래 이벤트 리스너
 * 
 * EconomyShop에서 발생하는 거래 이벤트를 감지하여
 * 직업 레벨 보너스와 아이템 품질 보너스를 적용합니다.
 * 
 * @author DreamWork Team
 */
public class ShopTransactionListener implements Listener {

    private final DreamWorkPlugin plugin;

    // 설정 키
    private static final String CONFIG_SHOP = "shop";
    private static final String CONFIG_JOB_BONUSES = "job-bonuses";
    private static final String CONFIG_QUALITY_MULTIPLIERS = "quality-multipliers";

    public ShopTransactionListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * EconomyShop 판매 이벤트 처리
     * 
     * 참고: EconomyShop이 이벤트를 지원하는 경우에만 작동합니다.
     * 만약 EconomyShop에서 커스텀 이벤트를 제공하지 않는다면
     * 별도의 연동 방식이 필요합니다.
     */
    // @EventHandler(priority = EventPriority.NORMAL)
    // public void onShopSell(ShopTransactionEvent event) {
    // // EconomyShop이 커스텀 이벤트를 지원하는 경우 여기서 구현
    // }

    // ==================== 직업 보너스 계산 ====================

    /**
     * 플레이어의 직업 레벨에 따른 판매 보너스 계산
     * 
     * @param player  대상 플레이어
     * @param jobType 직업 유형
     * @return 보너스 배율 (예: 1.0 = 보너스 없음, 1.25 = 25% 보너스)
     */
    public double calculateJobBonus(Player player, JobType jobType) {
        if (player == null || jobType == null) {
            return 1.0;
        }

        UUID playerId = player.getUniqueId();

        // UserData를 통해 레벨 가져오기
        com.dreamwork.core.UserData userData = plugin.getUserDataManager().getUserData(playerId);
        if (userData == null) {
            return 1.0;
        }

        int level = userData.getJobLevel(jobType);

        if (level <= 0) {
            return 1.0;
        }

        // 설정에서 보너스 값 가져오기
        String jobKey = jobType.name().toLowerCase();
        double bonusPerLevel = plugin.getConfigManager().getConfig()
                .getDouble(CONFIG_SHOP + "." + CONFIG_JOB_BONUSES + "." + jobKey + ".sell-bonus-per-level", 0.5);
        double maxBonus = plugin.getConfigManager().getConfig()
                .getDouble(CONFIG_SHOP + "." + CONFIG_JOB_BONUSES + "." + jobKey + ".max-bonus", 25.0);

        // 보너스 계산 (레벨 * 레벨당 보너스%)
        double bonus = Math.min(level * bonusPerLevel, maxBonus);

        plugin.debug(player.getName() + " " + jobType.name() + " 판매 보너스: " + bonus + "%");

        return 1.0 + (bonus / 100.0);
    }

    /**
     * 아이템에 해당하는 직업 유형 판단
     * 
     * @param item 아이템
     * @return 연관된 직업 유형 (없으면 null)
     */
    public JobType getRelatedJobType(ItemStack item) {
        if (item == null) {
            return null;
        }

        String materialName = item.getType().name();

        // 광물 관련 아이템 -> 광부
        if (materialName.contains("ORE") || materialName.contains("INGOT") ||
                materialName.contains("COAL") || materialName.contains("DIAMOND") ||
                materialName.contains("EMERALD") || materialName.contains("IRON") ||
                materialName.contains("GOLD") || materialName.contains("COPPER") ||
                materialName.contains("LAPIS") || materialName.contains("REDSTONE") ||
                materialName.contains("NETHERITE")) {
            return JobType.MINER;
        }

        // 농작물 관련 아이템 -> 농부
        if (materialName.contains("WHEAT") || materialName.contains("CARROT") ||
                materialName.contains("POTATO") || materialName.contains("BEETROOT") ||
                materialName.contains("MELON") || materialName.contains("PUMPKIN") ||
                materialName.contains("SUGAR_CANE") || materialName.contains("COCOA") ||
                materialName.contains("NETHER_WART") || materialName.equals("CACTUS") ||
                materialName.equals("BAMBOO")) {
            return JobType.FARMER;
        }

        // 물고기 관련 아이템 -> 어부
        if (materialName.contains("COD") || materialName.contains("SALMON") ||
                materialName.contains("TROPICAL_FISH") || materialName.contains("PUFFERFISH") ||
                materialName.contains("FISHING")) {
            return JobType.FISHER;
        }

        // 몹 드롭 관련 아이템 -> 사냥꾼
        if (materialName.contains("LEATHER") || materialName.contains("ROTTEN_FLESH") ||
                materialName.contains("BONE") || materialName.contains("SPIDER_EYE") ||
                materialName.contains("ENDER_PEARL") || materialName.contains("BLAZE_ROD") ||
                materialName.contains("GHAST_TEAR") || materialName.contains("GUNPOWDER") ||
                materialName.contains("ARROW") || materialName.contains("BOW") ||
                materialName.contains("STRING") || materialName.contains("FEATHER")) {
            return JobType.HUNTER;
        }

        // 탐험 관련 아이템 -> 탐험가
        if (materialName.contains("MAP") || materialName.contains("COMPASS") ||
                materialName.contains("CLOCK") || materialName.contains("SPYGLASS") ||
                materialName.contains("ECHO_SHARD") || materialName.contains("DISC_FRAGMENT")) {
            return JobType.ADVENTURER;
        }

        return null;
    }

    // ==================== 품질 보너스 계산 ====================

    /**
     * 아이템 품질에 따른 가격 배율 계산
     * 
     * @param item 아이템
     * @return 품질 배율 (예: 1.0 = 일반, 2.0 = 영웅급)
     */
    public double calculateQualityMultiplier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 1.0;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 품질 태그 확인 (DreamWork 아이템 시스템에서 설정)
        NamespacedKey qualityKey = new NamespacedKey(plugin, "quality");

        if (pdc.has(qualityKey, PersistentDataType.STRING)) {
            String quality = pdc.get(qualityKey, PersistentDataType.STRING);

            if (quality != null) {
                return getQualityMultiplier(quality.toLowerCase());
            }
        }

        // 별점 시스템 확인 (농부 품질 등)
        NamespacedKey starsKey = new NamespacedKey(plugin, "stars");

        if (pdc.has(starsKey, PersistentDataType.INTEGER)) {
            int stars = pdc.get(starsKey, PersistentDataType.INTEGER);
            return getStarsMultiplier(stars);
        }

        return 1.0;
    }

    /**
     * 품질 등급별 배율 가져오기
     */
    private double getQualityMultiplier(String quality) {
        ConfigurationSection config = plugin.getConfigManager().getConfig()
                .getConfigurationSection(CONFIG_SHOP + "." + CONFIG_QUALITY_MULTIPLIERS);

        if (config != null) {
            return config.getDouble(quality, 1.0);
        }

        // 기본값
        switch (quality) {
            case "legendary":
                return 3.0;
            case "epic":
                return 2.0;
            case "rare":
                return 1.5;
            case "uncommon":
                return 1.25;
            case "common":
            default:
                return 1.0;
        }
    }

    /**
     * 별점에 따른 배율 계산 (농부 작물 품질 시스템)
     */
    private double getStarsMultiplier(int stars) {
        switch (stars) {
            case 5:
                return 2.5;
            case 4:
                return 2.0;
            case 3:
                return 1.5;
            case 2:
                return 1.25;
            case 1:
            default:
                return 1.0;
        }
    }

    // ==================== 종합 가격 계산 ====================

    /**
     * 최종 판매 가격 계산 (기본가 + 직업 보너스 + 품질 보너스)
     * 
     * @param player    판매자
     * @param item      아이템
     * @param basePrice 기본 가격
     * @return 최종 가격
     */
    public double calculateFinalSellPrice(Player player, ItemStack item, double basePrice) {
        if (basePrice <= 0) {
            return 0;
        }

        // 1. 직업 보너스 적용
        JobType jobType = getRelatedJobType(item);
        double jobMultiplier = 1.0;

        if (jobType != null) {
            jobMultiplier = calculateJobBonus(player, jobType);
        }

        // 2. 품질 보너스 적용
        double qualityMultiplier = calculateQualityMultiplier(item);

        // 3. 최종 계산
        double finalPrice = basePrice * jobMultiplier * qualityMultiplier * item.getAmount();

        plugin.debug(String.format(
                "가격 계산: 기본=%.2f, 직업배율=%.2f, 품질배율=%.2f, 수량=%d, 최종=%.2f",
                basePrice, jobMultiplier, qualityMultiplier, item.getAmount(), finalPrice));

        return finalPrice;
    }

    /**
     * 아이템 판매 처리 (EconomyShop 통해)
     * 
     * @param player 판매자
     * @param item   판매할 아이템
     * @return 판매 성공 여부
     */
    public boolean sellItem(Player player, ItemStack item) {
        if (plugin.getEconomyShopHook() == null || !plugin.getEconomyShopHook().isEnabled()) {
            return false;
        }

        // 기본 가격 조회
        double basePrice = plugin.getEconomyShopHook().getSellPrice(item);

        if (basePrice <= 0) {
            player.sendMessage("§c이 아이템은 판매할 수 없습니다.");
            return false;
        }

        // 최종 가격 계산
        double finalPrice = calculateFinalSellPrice(player, item, basePrice);

        // Vault를 통해 돈 지급
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            plugin.getVaultHook().deposit(player, finalPrice);
            player.getInventory().removeItem(item);

            // 보너스 정보 표시
            JobType jobType = getRelatedJobType(item);
            double jobBonus = (jobType != null) ? (calculateJobBonus(player, jobType) - 1.0) * 100 : 0;
            double qualityBonus = (calculateQualityMultiplier(item) - 1.0) * 100;

            StringBuilder message = new StringBuilder();
            message.append("§e💰 §f").append(item.getAmount()).append("개를 판매하여 ")
                    .append("§6").append(String.format("%.1f", finalPrice)).append("G§f를 획득!");

            if (jobBonus > 0 || qualityBonus > 0) {
                message.append(" §7(");
                if (jobBonus > 0) {
                    message.append("직업 +").append(String.format("%.1f", jobBonus)).append("%");
                }
                if (qualityBonus > 0) {
                    if (jobBonus > 0)
                        message.append(", ");
                    message.append("품질 +").append(String.format("%.1f", qualityBonus)).append("%");
                }
                message.append(")");
            }

            player.sendMessage(message.toString());
            return true;
        }

        return false;
    }
}
