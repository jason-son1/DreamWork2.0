package com.dreamwork.gui;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * GUI 이벤트 리스너
 * 
 * 모든 DreamGui 관련 인벤토리 이벤트를 처리합니다.
 * 
 * @author DreamWork Team
 */
public class GuiListener implements Listener {

    private final DreamWorkPlugin plugin;

    // Debounce
    private final java.util.Map<java.util.UUID, Long> lastClick = new java.util.HashMap<>();
    private static final long COOLDOWN_MS = 200;

    public GuiListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 인벤토리 클릭 이벤트 처리
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        // Check Debounce
        long now = System.currentTimeMillis();
        if (now - lastClick.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) {
            event.setCancelled(true);
            return;
        }
        lastClick.put(player.getUniqueId(), now);

        // DreamGui인지 확인
        if (!(event.getInventory().getHolder() instanceof DreamGui gui)) {
            return;
        }

        // 기본적으로 아이템 이동 방지
        if (!gui.allowItemMovement()) {
            // 상단 인벤토리(GUI) 클릭
            if (event.getClickedInventory() == event.getInventory()) {
                // 입력 슬롯이 아니면 취소
                if (!gui.isInputSlot(event.getSlot())) {
                    event.setCancelled(true);
                }
            }
            // Shift 클릭으로 아이템 이동 시도
            else if (event.isShiftClick() && event.getClickedInventory() == player.getInventory()) {
                event.setCancelled(true);
            }
        }

        // GUI 클릭 핸들러 호출
        if (event.getClickedInventory() == event.getInventory()) {
            gui.onClick(event);
        }
    }

    /**
     * 인벤토리 드래그 이벤트 처리
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof DreamGui gui)) {
            return;
        }

        // GUI 영역에 드래그되었는지 확인
        if (!gui.allowItemMovement()) {
            for (int slot : event.getRawSlots()) {
                if (slot < gui.getSize()) {
                    if (!gui.isInputSlot(slot)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    /**
     * 인벤토리 닫기 이벤트 처리
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof DreamGui gui)) {
            return;
        }

        // 입력 슬롯에 남은 아이템 반환
        if (gui.allowItemMovement()) {
            Player player = (Player) event.getPlayer();

            for (int i = 0; i < gui.getSize(); i++) {
                if (gui.isInputSlot(i)) {
                    var item = event.getInventory().getItem(i);
                    if (item != null && !item.getType().isAir()) {
                        // 인벤토리에 추가, 꽉 차면 드롭
                        var remaining = player.getInventory().addItem(item);
                        for (var leftover : remaining.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                        }
                    }
                }
            }
        }
    }
}
