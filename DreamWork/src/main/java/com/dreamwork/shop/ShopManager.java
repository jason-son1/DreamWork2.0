package com.dreamwork.shop;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.gui.GuiButton;
import com.dreamwork.gui.GuiTemplate;
import com.dreamwork.gui.impl.DynamicGui;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * 상점 관리자
 * 
 * 동적 GUI 시스템을 활용하여 상점 기능을 제공합니다.
 * YAML 설정에서 가격 및 아이템 정보를 로드합니다.
 * 
 * @author DreamWork Team
 */
public class ShopManager {

    private final DreamWorkPlugin plugin;
    private final Map<String, ShopData> shops = new HashMap<>();

    public ShopManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        loadShops();
    }

    /**
     * 상점 데이터 로드 (GUI 설정 재활용)
     * GUI 설정 파일에 'shop_type: BUY_SELL' 등의 옵션이 있으면 상점으로 인식
     */
    public void loadShops() {
        shops.clear();
        Map<String, FileConfiguration> configs = plugin.getConfigManager().getAllGuiConfigs();

        for (Map.Entry<String, FileConfiguration> entry : configs.entrySet()) {
            String id = entry.getKey();
            FileConfiguration config = entry.getValue();

            // 상점 설정 확인
            String type = config.getString("shop_type");
            if (type != null) {
                ShopData shopData = new ShopData();
                shopData.setId(id);
                shopData.setType(type);

                // 판매/구매 가격 로드
                if (config.isConfigurationSection("prices")) {
                    ConfigurationSection prices = config.getConfigurationSection("prices");
                    for (String key : prices.getKeys(false)) {
                        shopData.setPrice(key, prices.getDouble(key));
                    }
                }

                shops.put(id, shopData);
                plugin.debug("상점 로드됨: " + id + " (타입: " + type + ")");
            }
        }
    }

    /**
     * 상점 열기
     */
    public void openShop(Player player, String shopId) {
        if (!shops.containsKey(shopId)) {
            // EconomyShop 상점 열기 시도
            if (openEconomyShop(player, shopId)) {
                return;
            }
            // 일반 GUI로 열기 시도
            plugin.getGuiManager().openGui(player, shopId);
            return;
        }

        // 상점 전용 GUI (DynamicGui 확장)
        ShopGui gui = new ShopGui(plugin, player, shopId);
        gui.open();
    }

    /**
     * EconomyShop 상점 열기
     * 
     * @param player    대상 플레이어
     * @param sectionId 섹션 ID ("farming", "mining", "fishing" 등)
     * @return 성공 여부
     */
    public boolean openEconomyShop(Player player, String sectionId) {
        if (plugin.getEconomyShopHook() == null || !plugin.getEconomyShopHook().isEnabled()) {
            return false;
        }
        return plugin.getEconomyShopHook().openShop(player, sectionId);
    }

    /**
     * EconomyShop 메인 메뉴 열기
     */
    public void openEconomyShopMenu(Player player) {
        if (plugin.getEconomyShopHook() == null || !plugin.getEconomyShopHook().isEnabled()) {
            player.sendMessage("§c상점 시스템이 비활성화되어 있습니다.");
            return;
        }
        plugin.getEconomyShopHook().openMainMenu(player);
    }

    /**
     * 아이템 판매 로직
     */
    public void sellItem(Player player, ItemStack item, double price) {
        if (plugin.getVaultHook() == null)
            return;

        double total = price * item.getAmount();
        plugin.getVaultHook().deposit(player, total);
        player.getInventory().removeItem(item);

        player.sendMessage("§e💰 " + item.getAmount() + "개를 판매하여 " + String.format("%.1f", total) + "G를 획득했습니다.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public ShopData getShop(String id) {
        return shops.get(id);
    }

    /**
     * 직업 레벨에 따른 판매 보너스 배율 계산
     * 
     * @param player  플레이어
     * @param jobType 직업 타입
     * @return 보너스 배율 (예: 1.0 = 보너스 없음, 1.25 = 25% 보너스)
     */
    public double calculateJobSellBonus(Player player, com.dreamwork.job.JobType jobType) {
        if (player == null || jobType == null) {
            return 1.0;
        }

        com.dreamwork.core.UserData userData = plugin.getUserDataManager().getUserData(player.getUniqueId());
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
                .getDouble("shop.job-bonuses." + jobKey + ".sell-bonus-per-level", 0.5);
        double maxBonus = plugin.getConfigManager().getConfig()
                .getDouble("shop.job-bonuses." + jobKey + ".max-bonus", 25.0);

        double bonus = Math.min(level * bonusPerLevel, maxBonus);
        return 1.0 + (bonus / 100.0);
    }

    /**
     * 아이템 품질에 따른 가격 배율 계산
     */
    public double calculateQualityMultiplier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 1.0;
        }

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 품질 태그 확인
        org.bukkit.NamespacedKey qualityKey = new org.bukkit.NamespacedKey(plugin, "quality");
        if (pdc.has(qualityKey, org.bukkit.persistence.PersistentDataType.STRING)) {
            String quality = pdc.get(qualityKey, org.bukkit.persistence.PersistentDataType.STRING);
            return getQualityMultiplier(quality);
        }

        // 별점 시스템 확인 (농부 품질)
        org.bukkit.NamespacedKey starsKey = new org.bukkit.NamespacedKey(plugin, "stars");
        if (pdc.has(starsKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
            int stars = pdc.get(starsKey, org.bukkit.persistence.PersistentDataType.INTEGER);
            return getStarsMultiplier(stars);
        }

        return 1.0;
    }

    private double getQualityMultiplier(String quality) {
        if (quality == null)
            return 1.0;

        org.bukkit.configuration.ConfigurationSection config = plugin.getConfigManager().getConfig()
                .getConfigurationSection("shop.quality-multipliers");
        if (config != null) {
            return config.getDouble(quality.toLowerCase(), 1.0);
        }

        // 기본값
        switch (quality.toLowerCase()) {
            case "legendary":
                return 3.0;
            case "epic":
                return 2.0;
            case "rare":
                return 1.5;
            case "uncommon":
                return 1.25;
            default:
                return 1.0;
        }
    }

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
            default:
                return 1.0;
        }
    }

    // 내부 데이터 클래스
    public static class ShopData {
        private String id;
        private String type; // BUY, SELL, BOTH
        private final Map<String, Double> prices = new HashMap<>();

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setPrice(String itemId, double price) {
            prices.put(itemId, price);
        }

        public double getPrice(String itemId) {
            return prices.getOrDefault(itemId, 0.0);
        }
    }
}
