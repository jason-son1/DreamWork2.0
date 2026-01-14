package com.dreamwork.hook;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.shop.injector.DreamShopConfig;
import com.dreamwork.shop.injector.ShopInjector;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * EconomyShop 플러그인 Hook (리플렉션 기반)
 * 
 * 리플렉션을 사용하여 EconomyShop API를 호출합니다.
 * 컴파일 타임 의존성 없이 런타임에 연동하여 버전 호환성을 유지합니다.
 * 
 * 주요 기능:
 * - 상점 GUI 열기
 * - 가격 조회
 * - DreamWork 상점 주입 (ShopInjector 활용)
 * 
 * @author DreamWork Team
 */
public class EconomyShopHook {

    private final DreamWorkPlugin plugin;
    private Object economyShop;
    private ShopInjector shopInjector;
    private boolean enabled = false;

    public EconomyShopHook(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * EconomyShop 연동 초기화
     */
    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("EconomyShop") == null) {
            plugin.log(Level.INFO, "EconomyShop 플러그인을 찾을 수 없습니다. 상점 연동이 비활성화됩니다.");
            return false;
        }

        try {
            Class<?> esClass = Class.forName("me.antigravity.economyshop.EconomyShop");
            economyShop = esClass.getMethod("getInstance").invoke(null);
            enabled = (economyShop != null);

            if (enabled) {
                plugin.debug("EconomyShop 연동 완료");
            }
            return enabled;

        } catch (ClassNotFoundException e) {
            plugin.log(Level.INFO, "EconomyShop 클래스를 찾을 수 없습니다.");
            return false;
        } catch (Exception e) {
            plugin.log(Level.WARNING, "EconomyShop 연동 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 연동 상태 확인
     */
    public boolean isEnabled() {
        return enabled && economyShop != null;
    }

    // ==================== 상점 주입 관련 ====================

    /**
     * DreamWork 상점을 EconomyShop에 주입
     */
    public void injectShops() {
        if (!isEnabled()) {
            plugin.log(Level.WARNING, "EconomyShop이 비활성화되어 상점을 주입할 수 없습니다.");
            return;
        }

        if (shopInjector == null) {
            shopInjector = new ShopInjector(plugin);
        }
        shopInjector.injectAllShops();
    }

    /**
     * 상점 리로드 (기존 제거 후 재주입)
     */
    public void reloadShops() {
        if (shopInjector != null) {
            shopInjector.unloadAllShops();
        }
        injectShops();
    }

    /**
     * 상점 주입기 가져오기
     */
    public ShopInjector getShopInjector() {
        return shopInjector;
    }

    /**
     * 특정 상점 설정 가져오기
     */
    public DreamShopConfig getShopConfig(String sectionId) {
        return shopInjector != null ? shopInjector.getShopConfig(sectionId) : null;
    }

    // ==================== GUI 관련 ====================

    /**
     * 메인 상점 메뉴 열기
     */
    public void openMainMenu(Player player) {
        if (!isEnabled()) {
            player.sendMessage("§c상점 시스템이 비활성화되어 있습니다.");
            return;
        }

        try {
            Object guiManager = economyShop.getClass().getMethod("getGuiManager").invoke(economyShop);
            guiManager.getClass().getMethod("openMainMenu", Player.class).invoke(guiManager, player);
            plugin.debug(player.getName() + "에게 상점 메인 메뉴 열기");
        } catch (Exception e) {
            plugin.debug("상점 메뉴 열기 실패: " + e.getMessage());
            player.sendMessage("§c상점을 열 수 없습니다.");
        }
    }

    /**
     * 특정 섹션 상점 열기
     */
    public boolean openShop(Player player, String sectionId) {
        if (!isEnabled()) {
            player.sendMessage("§c상점 시스템이 비활성화되어 있습니다.");
            return false;
        }

        try {
            Object shopManager = economyShop.getClass().getMethod("getShopManager").invoke(economyShop);
            Object sections = shopManager.getClass().getMethod("getSections").invoke(shopManager);

            @SuppressWarnings("unchecked")
            Map<String, Object> sectionMap = (Map<String, Object>) sections;
            Object section = sectionMap.get(sectionId);

            if (section == null) {
                plugin.debug("상점 섹션을 찾을 수 없음: " + sectionId);
                return false;
            }

            Object guiManager = economyShop.getClass().getMethod("getGuiManager").invoke(economyShop);

            // openShop 메서드 찾기
            for (Method method : guiManager.getClass().getMethods()) {
                if (method.getName().equals("openShop") && method.getParameterCount() == 2) {
                    method.invoke(guiManager, player, section);
                    plugin.debug(player.getName() + "에게 " + sectionId + " 상점 열기");
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            plugin.debug("상점 열기 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 직업별 상점 열기 (편의 메서드)
     */
    public boolean openJobShop(Player player, String jobName) {
        String sectionId = "dw_" + jobName.toLowerCase();
        return openShop(player, sectionId);
    }

    // ==================== 가격 조회 ====================

    /**
     * 아이템 판매 가격 조회
     */
    public double getSellPrice(ItemStack item) {
        if (!isEnabled() || item == null) {
            return -1;
        }

        try {
            Object shopManager = economyShop.getClass().getMethod("getShopManager").invoke(economyShop);
            Object sections = shopManager.getClass().getMethod("getSections").invoke(shopManager);

            @SuppressWarnings("unchecked")
            Map<String, Object> sectionMap = (Map<String, Object>) sections;

            for (Object section : sectionMap.values()) {
                @SuppressWarnings("unchecked")
                List<Object> items = (List<Object>) section.getClass().getMethod("getItems").invoke(section);

                for (Object shopItem : items) {
                    String itemId = (String) shopItem.getClass().getMethod("getId").invoke(shopItem);

                    if (itemId.equalsIgnoreCase(item.getType().name())) {
                        return (double) shopItem.getClass().getMethod("getCurrentSellPrice").invoke(shopItem);
                    }
                }
            }
        } catch (Exception e) {
            plugin.debug("가격 조회 실패: " + e.getMessage());
        }

        return -1;
    }

    /**
     * 아이템 구매 가격 조회
     */
    public double getBuyPrice(ItemStack item) {
        if (!isEnabled() || item == null) {
            return -1;
        }

        try {
            Object shopManager = economyShop.getClass().getMethod("getShopManager").invoke(economyShop);
            Object sections = shopManager.getClass().getMethod("getSections").invoke(shopManager);

            @SuppressWarnings("unchecked")
            Map<String, Object> sectionMap = (Map<String, Object>) sections;

            for (Object section : sectionMap.values()) {
                @SuppressWarnings("unchecked")
                List<Object> items = (List<Object>) section.getClass().getMethod("getItems").invoke(section);

                for (Object shopItem : items) {
                    String itemId = (String) shopItem.getClass().getMethod("getId").invoke(shopItem);

                    if (itemId.equalsIgnoreCase(item.getType().name())) {
                        return (double) shopItem.getClass().getMethod("getCurrentBuyPrice").invoke(shopItem);
                    }
                }
            }
        } catch (Exception e) {
            plugin.debug("가격 조회 실패: " + e.getMessage());
        }

        return -1;
    }

    /**
     * 특정 섹션에서 아이템 가격 조회
     */
    public double getSellPrice(String sectionId, String itemId) {
        if (!isEnabled()) {
            return -1;
        }

        try {
            Object shopManager = economyShop.getClass().getMethod("getShopManager").invoke(economyShop);
            Object sections = shopManager.getClass().getMethod("getSections").invoke(shopManager);

            @SuppressWarnings("unchecked")
            Map<String, Object> sectionMap = (Map<String, Object>) sections;
            Object section = sectionMap.get(sectionId);

            if (section != null) {
                @SuppressWarnings("unchecked")
                List<Object> items = (List<Object>) section.getClass().getMethod("getItems").invoke(section);

                for (Object shopItem : items) {
                    String shopItemId = (String) shopItem.getClass().getMethod("getId").invoke(shopItem);

                    if (shopItemId.equalsIgnoreCase(itemId)) {
                        return (double) shopItem.getClass().getMethod("getCurrentSellPrice").invoke(shopItem);
                    }
                }
            }
        } catch (Exception e) {
            plugin.debug("가격 조회 실패: " + e.getMessage());
        }

        return -1;
    }

    // ==================== 유틸리티 ====================

    /**
     * 아이템이 판매 가능한지 확인
     */
    public boolean isSellable(ItemStack item) {
        return getSellPrice(item) > 0;
    }

    /**
     * 섹션 존재 여부 확인
     */
    public boolean hasSection(String sectionId) {
        if (!isEnabled()) {
            return false;
        }

        try {
            Object shopManager = economyShop.getClass().getMethod("getShopManager").invoke(economyShop);
            Object sections = shopManager.getClass().getMethod("getSections").invoke(shopManager);

            @SuppressWarnings("unchecked")
            Map<String, Object> sectionMap = (Map<String, Object>) sections;
            return sectionMap.containsKey(sectionId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * EconomyShop 인스턴스 가져오기 (리플렉션용)
     */
    public Object getEconomyShop() {
        return economyShop;
    }
}
