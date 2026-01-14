package com.dreamwork.hook;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * EconomyShop 플러그인 Hook (간소화 버전)
 * 
 * 리플렉션 기반으로 EconomyShop API를 호출하여
 * 컴파일 타임 의존성 없이 런타임에 연동합니다.
 * 
 * 실제 EconomyShop API 구조:
 * - me.antigravity.economyshop.EconomyShop (메인 플러그인)
 * - me.antigravity.economyshop.model.ShopItem, ShopSection (데이터 모델)
 * - me.antigravity.economyshop.manager.ShopManager, GUIManager (매니저)
 * 
 * @author DreamWork Team
 */
public class EconomyShopHook {

    private final DreamWorkPlugin plugin;
    private Object economyShop; // Object로 선언하여 컴파일 의존성 방지
    private boolean enabled = false;

    public EconomyShopHook(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * EconomyShop 연동 초기화
     * 
     * @return 성공 여부
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
            plugin.log(Level.INFO, "EconomyShop 클래스를 찾을 수 없습니다. 상점 연동이 비활성화됩니다.");
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

    // ==================== GUI 관련 ====================

    /**
     * 메인 상점 메뉴 열기
     * 
     * @param player 대상 플레이어
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
     * 
     * @param player    대상 플레이어
     * @param sectionId 섹션 ID (예: "farming", "mining", "fishing")
     * @return 성공 여부
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
            guiManager.getClass().getMethod("openShop", Player.class, section.getClass())
                    .invoke(guiManager, player, section);

            plugin.debug(player.getName() + "에게 " + sectionId + " 상점 열기");
            return true;

        } catch (Exception e) {
            plugin.debug("상점 열기 실패: " + e.getMessage());
            return false;
        }
    }

    // ==================== 가격 조회 ====================

    /**
     * 아이템 판매 가격 조회 (동적 가격 지원)
     * 
     * @param item 아이템
     * @return 판매 가격 (등록되지 않은 경우 -1)
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
                        // 동적 가격 사용 (getCurrentSellPrice)
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
     * 아이템 구매 가격 조회 (동적 가격 지원)
     * 
     * @param item 아이템
     * @return 구매 가격 (등록되지 않은 경우 -1)
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
                        // 동적 가격 사용 (getCurrentBuyPrice)
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
     * 
     * @param sectionId 섹션 ID
     * @param itemId    아이템 ID (Material 이름)
     * @return 판매 가격 (없으면 -1)
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
     * 
     * @param item 아이템
     * @return 판매 가능 여부
     */
    public boolean isSellable(ItemStack item) {
        return getSellPrice(item) > 0;
    }

    /**
     * 섹션 존재 여부 확인
     * 
     * @param sectionId 섹션 ID
     * @return 존재 여부
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
     * 아이템이 상점에 등록되어 있는지 확인
     * 
     * @param itemId 아이템 ID (Material 이름)
     * @return 등록 여부
     */
    public boolean hasShopItem(String itemId) {
        if (!isEnabled()) {
            return false;
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
                    String shopItemId = (String) shopItem.getClass().getMethod("getId").invoke(shopItem);

                    if (shopItemId.equalsIgnoreCase(itemId)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            plugin.debug("아이템 확인 실패: " + e.getMessage());
        }

        return false;
    }
}
