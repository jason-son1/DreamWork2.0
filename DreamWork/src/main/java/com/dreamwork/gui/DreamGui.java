package com.dreamwork.gui;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * DreamWork GUI 추상 클래스
 * 
 * 모든 커스텀 GUI의 부모 클래스입니다.
 * InventoryHolder를 구현하여 이 GUI가 DreamWork의 것임을 증명합니다.
 * 
 * @author DreamWork Team
 */
public abstract class DreamGui implements InventoryHolder {

    protected final DreamWorkPlugin plugin;
    protected final Player player;
    protected Inventory inventory;
    protected String title;
    protected int size;

    /**
     * GUI 생성자
     * 
     * @param plugin 플러그인 인스턴스
     * @param player GUI를 볼 플레이어
     * @param title  GUI 제목
     * @param rows   행 수 (1~6)
     */
    public DreamGui(DreamWorkPlugin plugin, Player player, String title, int rows) {
        this.plugin = plugin;
        this.player = player;
        this.title = title;
        this.size = rows * 9;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    /**
     * GUI 초기화 (아이템 배치)
     * 하위 클래스에서 구현
     */
    public abstract void initialize();

    /**
     * 클릭 이벤트 처리
     * 하위 클래스에서 구현
     * 
     * @param event 클릭 이벤트
     */
    public abstract void onClick(InventoryClickEvent event);

    /**
     * GUI 열기
     */
    public void open() {
        initialize();
        player.openInventory(inventory);
    }

    /**
     * GUI 내용 새로고침
     */
    public void refresh() {
        inventory.clear();
        initialize();
    }

    /**
     * GUI 닫기
     */
    public void close() {
        player.closeInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    /**
     * 이 GUI에서 아이템 이동을 허용하는지 여부
     * 기본값은 false (이동 불가)
     */
    public boolean allowItemMovement() {
        return false;
    }

    /**
     * 특정 슬롯이 입력 슬롯인지 확인
     * 입력 슬롯은 아이템 이동이 허용됨
     */
    public boolean isInputSlot(int slot) {
        return false;
    }

    /**
     * 간단한 아이템 생성 헬퍼 메서드
     */
    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
