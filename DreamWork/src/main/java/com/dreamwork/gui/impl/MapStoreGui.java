package com.dreamwork.gui.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.gui.DreamGui;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 지도 상점 GUI
 * 
 * 탐험가가 사용하는 시설입니다.
 * - 바이옴 지도 구매
 * - 유적 지도 구매
 * 
 * @author DreamWork Team
 */
public class MapStoreGui extends DreamGui {

    private final DreamWorkPlugin plugin;

    public MapStoreGui(DreamWorkPlugin plugin, Player player) {
        super(plugin, player, "§2🗺 지도 상점", 3);
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        // 배경
        ItemStack glass = createGlassPane(Material.GREEN_STAINED_GLASS_PANE);
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, glass);
        }

        // 지도 목록
        inventory.setItem(11, createMapItem("§a[ 🌲 숲 바이옴 지도 ]", "가까운 숲을 찾습니다.", 100));
        inventory.setItem(13, createMapItem("§b[ ❄ 설원 바이옴 지도 ]", "가까운 설원을 찾습니다.", 200));
        inventory.setItem(15, createMapItem("§6[ 🏜 사막 바이옴 지도 ]", "가까운 사막을 찾습니다.", 150));

        // 뒤로 가기
        inventory.setItem(18, createBackButton());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == 18) {
            plugin.getGuiManager().openMainDashboard(player);
            return;
        }

        // 지도 구매 로직 (간단 구현)
        if (slot == 11 || slot == 13 || slot == 15) {
            player.sendMessage("§e지도 구매 기능은 아직 개발 중입니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private ItemStack createMapItem(String name, String desc, int price) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of("§7" + desc, "§7", "§e가격: §f" + price + "D", "§7", "§e클릭하여 구매"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c[ ← 뒤로 가기 ]");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlassPane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
}
