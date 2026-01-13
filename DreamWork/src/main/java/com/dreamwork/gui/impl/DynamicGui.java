package com.dreamwork.gui.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.gui.DreamGui;
import com.dreamwork.gui.GuiButton;
import com.dreamwork.gui.GuiTemplate;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 동적 GUI
 * 
 * GuiTemplate을 기반으로 생성되는 GUI입니다.
 * YAML 설정에 따라 레이아웃과 동작이 결정됩니다.
 * 
 * @author DreamWork Team
 */
public class DynamicGui extends DreamGui {

    private final GuiTemplate template;
    private final Map<Integer, List<String>> slotActions = new HashMap<>();

    public DynamicGui(DreamWorkPlugin plugin, Player player, GuiTemplate template) {
        super(plugin, player, template.getTitle(), template.getRows());
        this.template = template;
    }

    @Override
    public void initialize() {
        // 타이틀 플레이스홀더 처리 (생성자 호출 이전에 처리할 수 없으므로, 필요시 리플렉션이나 별도 타이틀 변경 메서드 필요하지만
        // Bukkit API 한계로 타이틀은 최초 생성 시 고정됩니다. PAPI 지원을 위해선 GuiManager에서 타이틀 파싱 후 생성자에
        // 전달해야 함.
        // 여기서는 간단히 super()에서 타이틀을 받았으므로 패스)

        // 배경 채우기
        if (template.getFillItem() != null && template.getFillItem() != Material.AIR) {
            ItemStack fillItem = createItem(template.getFillItem(), " ", List.of());
            for (int i = 0; i < size; i++) {
                inventory.setItem(i, fillItem);
            }
        }

        // 버튼 배치
        if (template.getButtons() != null) {
            for (GuiButton button : template.getButtons().values()) {
                // 조건 체크
                if (button.getCondition() != null && !checkCondition(player, button.getCondition())) {
                    continue;
                }

                ItemStack item = buildButtonParams(button);

                for (int slot : button.getSlots()) {
                    if (slot >= 0 && slot < size) {
                        inventory.setItem(slot, item);

                        // 액션 등록
                        if (button.getActions() != null && !button.getActions().isEmpty()) {
                            slotActions.put(slot, button.getActions());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getSlot();
        if (slotActions.containsKey(slot)) {
            List<String> actions = slotActions.get(slot);
            executeActions(actions);
        }
    }

    /**
     * 액션 목록 실행
     */
    private void executeActions(List<String> actions) {
        for (String actionLine : actions) {
            String[] parts = actionLine.split(":", 2);
            String type = parts[0].toUpperCase();
            String value = parts.length > 1 ? parts[1] : "";

            // 플레이스홀더 파싱
            value = value.replace("%player%", player.getName());

            switch (type) {
                case "OPEN":
                    plugin.getGuiManager().openGui(player, value);
                    break;
                case "CMD":
                    player.performCommand(value);
                    break;
                case "CONSOLE":
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), value);
                    break;
                case "CLOSE":
                    player.closeInventory();
                    break;
                case "SOUND":
                    try {
                        player.playSound(player.getLocation(), Sound.valueOf(value), 1.0f, 1.0f);
                    } catch (IllegalArgumentException ignored) {
                    }
                    break;
                case "MSG":
                    player.sendMessage(value.replace("&", "§"));
                    break;
                // 추가 액션 필요시 구현
            }
        }
    }

    /**
     * 버튼 아이템 생성
     */
    private ItemStack buildButtonParams(GuiButton button) {
        ItemStack item = new ItemStack(button.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(translatePlaceholders(button.getName()));

            List<String> lore = new ArrayList<>();
            if (button.getLore() != null) {
                for (String line : button.getLore()) {
                    lore.add(translatePlaceholders(line));
                }
            }
            meta.setLore(lore);

            if (button.getCustomModelData() > 0) {
                meta.setCustomModelData(button.getCustomModelData());
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 플레이스홀더 변환 (간단 구현)
     * 실제로는 PAPI를 써야 함
     */
    private String translatePlaceholders(String text) {
        if (text == null)
            return "";
        String result = text.replace("&", "§")
                .replace("%player%", player.getName());

        // TODO: PlaceholderAPI 연동
        // if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
        // {
        // result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player,
        // result);
        // }

        return result;
    }

    /**
     * 조건 체크
     */
    private boolean checkCondition(Player player, String condition) {
        // 예: "permission:dreamwork.admin"
        if (condition.startsWith("permission:")) {
            String perm = condition.substring("permission:".length());
            return player.hasPermission(perm);
        }
        return true;
    }
}
