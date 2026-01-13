package com.dreamwork.gui.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.dialogue.DialogueNode;
import com.dreamwork.dialogue.DialogueOption;
import com.dreamwork.dialogue.DialogueSession;
import com.dreamwork.gui.DreamGui;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 대화 GUI
 * 
 * NPC 대화를 표시하는 GUI입니다.
 * 
 * @author DreamWork Team
 */
public class DialogueGui extends DreamGui {

    private final DreamWorkPlugin plugin;
    private final DialogueSession session;

    // 슬롯 정의
    private static final int SPEAKER_SLOT = 4;
    private static final int TEXT_SLOT = 13;
    private static final int[] OPTION_SLOTS = { 28, 30, 32, 34 }; // 최대 4개 선택지

    public DialogueGui(DreamWorkPlugin plugin, Player player, DialogueSession session) {
        super(plugin, player, "§8대화", 5);
        this.plugin = plugin;
        this.session = session;
    }

    @Override
    public void initialize() {
        // 배경
        ItemStack glass = createGlassPane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, glass);
        }

        DialogueNode node = session.getCurrentNode();
        if (node == null)
            return;

        // 화자 표시
        inventory.setItem(SPEAKER_SLOT, createSpeakerItem(node.getSpeaker()));

        // 대사 표시
        inventory.setItem(TEXT_SLOT, createTextItem(node.getText()));

        // 선택지 표시
        List<DialogueOption> options = node.getOptions();
        for (int i = 0; i < OPTION_SLOTS.length && i < options.size(); i++) {
            DialogueOption opt = options.get(i);

            // 조건 체크
            boolean enabled = plugin.getDialogueManager().checkCondition(player, opt.getCondition());

            inventory.setItem(OPTION_SLOTS[i], createOptionItem(i, opt, enabled));
        }

        // 대화 종료 노드면 닫기 버튼만 표시
        if (node.isEndNode()) {
            inventory.setItem(31, createCloseButton());
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();

        // 선택지 클릭 처리
        for (int i = 0; i < OPTION_SLOTS.length; i++) {
            if (slot == OPTION_SLOTS[i]) {
                DialogueNode node = session.getCurrentNode();
                if (node != null && i < node.getOptions().size()) {
                    DialogueOption opt = node.getOptions().get(i);

                    // 조건 체크
                    if (!plugin.getDialogueManager().checkCondition(player, opt.getCondition())) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }

                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    plugin.getDialogueManager().selectOption(player, i);
                }
                return;
            }
        }

        // 닫기 버튼
        if (slot == 31) {
            plugin.getDialogueManager().endDialogue(player);
        }
    }

    private ItemStack createSpeakerItem(String speaker) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + speaker);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTextItem(String text) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§f" + plugin.getConfigManager().translateColors(text));

        // 긴 텍스트는 lore로 분할
        List<String> lore = new ArrayList<>();
        String translated = plugin.getConfigManager().translateColors(text);
        if (translated.length() > 40) {
            String[] words = translated.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (line.length() + word.length() > 35) {
                    lore.add("§f" + line.toString().trim());
                    line = new StringBuilder();
                }
                line.append(word).append(" ");
            }
            if (line.length() > 0) {
                lore.add("§f" + line.toString().trim());
            }
            meta.setDisplayName("§f「 대화 」");
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOptionItem(int index, DialogueOption option, boolean enabled) {
        Material mat = enabled ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        String prefix = enabled ? "§a" : "§8";
        meta.setDisplayName(prefix + "[" + (index + 1) + "] " + option.getText());

        if (!enabled) {
            meta.setLore(List.of("§c조건을 충족하지 않습니다."));
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c[ 대화 종료 ]");
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
