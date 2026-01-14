package com.dreamwork.gui.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.gui.DreamGui;
import com.dreamwork.shop.injector.DreamShopConfig;
import com.dreamwork.shop.injector.ShopInjector;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.*;

/**
 * 상점 관리자 GUI
 * 
 * 관리자가 인게임에서 DreamWork 상점을 관리할 수 있는 GUI입니다.
 * 
 * @author DreamWork Team
 */
public class ShopAdminGui extends DreamGui {

    private static final int[] SHOP_SLOTS = { 10, 11, 12, 13, 14, 19, 20, 21, 22, 23 };
    private final Map<Integer, String> slotToShopId = new HashMap<>();

    public ShopAdminGui(DreamWorkPlugin plugin, Player player) {
        super(plugin, player, "§6§l[ 상점 관리 ]", 6);
    }

    @Override
    public void initialize() {
        // 배경 유리판
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));
        }
        for (int i = 45; i < 54; i++) {
            if (i != 45 && i != 49 && i != 53) {
                inventory.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));
            }
        }

        // 헤더
        inventory.setItem(4, createItem(Material.GOLD_BLOCK, "§6상점 관리",
                Arrays.asList(
                        "§7DreamWork 상점을 관리합니다.",
                        "",
                        "§e좌클릭: §f상점 열기",
                        "§e우클릭: §f설정 파일 경로 보기")));

        // 등록된 상점 목록 표시
        displayShops();

        // 하단 메뉴
        inventory.setItem(45, createItem(Material.ARROW, "§c닫기", List.of("§7메뉴를 닫습니다.")));
        inventory.setItem(49, createItem(Material.EMERALD, "§a상점 리로드",
                Arrays.asList("§7모든 상점 설정을 다시 불러옵니다.", "", "§e클릭하여 리로드")));
        inventory.setItem(53, createItem(Material.BOOK, "§e도움말",
                Arrays.asList(
                        "§7상점 아이템/가격 수정 방법:",
                        "",
                        "§f1. §7plugins/DreamWork/shops/ 폴더 열기",
                        "§f2. §7원하는 상점 YAML 파일 편집",
                        "§f3. §7/dw shop reload 명령어 실행",
                        "",
                        "§6YAML 형식 예시:",
                        "§7- id: \"stone\"",
                        "§7  material: STONE",
                        "§7  slot: 10",
                        "§7  buy_price: 10.0",
                        "§7  sell_price: 5.0")));
    }

    /**
     * 등록된 상점 목록 표시
     */
    private void displayShops() {
        slotToShopId.clear();

        if (plugin.getEconomyShopHook() == null || !plugin.getEconomyShopHook().isEnabled()) {
            inventory.setItem(22, createItem(Material.BARRIER, "§c상점 시스템 비활성화",
                    List.of("§7EconomyShop 연동이 필요합니다.")));
            return;
        }

        ShopInjector injector = plugin.getEconomyShopHook().getShopInjector();
        if (injector == null) {
            inventory.setItem(22, createItem(Material.BARRIER, "§c상점 주입기 없음",
                    List.of("§7상점이 아직 초기화되지 않았습니다.")));
            return;
        }

        Set<String> shopIds = injector.getInjectedSectionIds();
        int slotIndex = 0;

        for (String shopId : shopIds) {
            if (slotIndex >= SHOP_SLOTS.length)
                break;

            DreamShopConfig config = injector.getShopConfig(shopId);
            int slot = SHOP_SLOTS[slotIndex++];

            Material icon = config != null && config.getIcon() != null ? config.getIcon() : Material.CHEST;
            String displayName = config != null ? ChatColor.translateAlternateColorCodes('&', config.getDisplayName())
                    : shopId;
            int itemCount = config != null ? config.getItems().size() : 0;

            List<String> lore = Arrays.asList(
                    "§7ID: §f" + shopId,
                    "§7아이템 수: §f" + itemCount + "개",
                    "",
                    "§e좌클릭: §f상점 열기",
                    "§e우클릭: §f설정 파일 경로");

            inventory.setItem(slot, createItem(icon, displayName, lore));
            slotToShopId.put(slot, shopId);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getSlot();
        ClickType clickType = event.getClick();

        // 닫기 버튼
        if (slot == 45) {
            player.closeInventory();
            return;
        }

        // 리로드 버튼
        if (slot == 49) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            if (plugin.getEconomyShopHook() != null) {
                plugin.getEconomyShopHook().reloadShops();
                player.sendMessage("§a상점 설정이 리로드되었습니다!");
            }
            // 다시 열기
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getGuiManager().openShopAdminGui(player);
            }, 5L);
            return;
        }

        // 상점 슬롯 클릭
        if (slotToShopId.containsKey(slot)) {
            String shopId = slotToShopId.get(slot);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

            if (clickType == ClickType.LEFT) {
                // 좌클릭: 상점 열기
                player.closeInventory();
                plugin.getEconomyShopHook().openShop(player, shopId);
            } else if (clickType == ClickType.RIGHT) {
                // 우클릭: 설정 파일 경로 표시
                String fileName = shopId.replace("dw_", "") + "_shop.yml";
                player.sendMessage("§6=== 상점 설정 파일 ===");
                player.sendMessage("§7경로: §fplugins/DreamWork/shops/" + fileName);
                player.sendMessage("§7파일을 수정한 후 §e/dw shop reload §7명령어를 실행하세요.");
            }
        }
    }
}
