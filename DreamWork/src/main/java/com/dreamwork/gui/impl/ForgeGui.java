package com.dreamwork.gui.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.gui.DreamGui;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 대장간 GUI
 * 
 * 광부가 사용하는 시설입니다.
 * - 미지의 광석 감정
 * - 합금 제작
 * - 도구 강화
 * 
 * @author DreamWork Team
 */
public class ForgeGui extends DreamGui {

    private final DreamWorkPlugin plugin;

    // 입력 슬롯 (아이템을 넣을 수 있는 슬롯)
    private static final int INPUT_SLOT = 10;
    private static final int OUTPUT_SLOT = 16;
    private static final int PROCESS_BUTTON_SLOT = 13;

    public ForgeGui(DreamWorkPlugin plugin, Player player) {
        super(plugin, player, plugin.getConfigManager().getMessage("gui.forge-title"), 4);
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        // 배경 채우기
        ItemStack glass = createGlassPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 36; i++) {
            if (i != INPUT_SLOT && i != OUTPUT_SLOT && i != PROCESS_BUTTON_SLOT) {
                inventory.setItem(i, glass);
            }
        }

        // 화살표 장식
        inventory.setItem(11, createArrowPane());
        inventory.setItem(12, createArrowPane());
        inventory.setItem(14, createArrowPane());
        inventory.setItem(15, createArrowPane());

        // 처리 버튼
        inventory.setItem(PROCESS_BUTTON_SLOT, createProcessButton());

        // 뒤로 가기
        inventory.setItem(27, createBackButton());

        // 메뉴 탭
        inventory.setItem(29, createIdentifyTab());
        inventory.setItem(31, createAlloyTab());
        inventory.setItem(33, createUpgradeTab());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        // 입력/출력 슬롯은 아이템 이동 허용
        if (slot == INPUT_SLOT || slot == OUTPUT_SLOT) {
            return; // 취소하지 않음
        }

        event.setCancelled(true);

        switch (slot) {
            case PROCESS_BUTTON_SLOT -> processItem();
            case 27 -> plugin.getGuiManager().openJobDetailGui(player,
                    com.dreamwork.job.JobType.MINER);
            case 29 -> player.sendMessage("§e감정 기능 선택됨");
            case 31 -> player.sendMessage("§e합금 제작 기능 (개발 중)");
            case 33 -> player.sendMessage("§e도구 강화 기능 (개발 중)");
        }
    }

    /**
     * 아이템 처리 (감정)
     */
    private void processItem() {
        ItemStack inputItem = inventory.getItem(INPUT_SLOT);

        if (inputItem == null || inputItem.getType().isAir()) {
            player.sendMessage("§c감정할 아이템을 왼쪽 슬롯에 넣어주세요.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        String itemId = plugin.getItemManager().getDreamItemId(inputItem);

        // 미지의 광석인 경우 감정
        if (itemId != null && itemId.equals("unknown_ore")) {
            // 비용 확인 (예: 10D)
            double cost = 10.0;
            if (plugin.getVaultHook() != null && !plugin.getVaultHook().has(player, cost)) {
                player.sendMessage("§c감정 비용이 부족합니다. (필요: " + cost + "D)");
                return;
            }

            // 비용 차감
            if (plugin.getVaultHook() != null) {
                plugin.getVaultHook().withdraw(player, cost);
            }

            // 결과물 생성 (랜덤)
            ItemStack result = generateIdentificationResult();

            // 입력 아이템 감소
            inputItem.setAmount(inputItem.getAmount() - 1);

            // 출력 슬롯에 결과 배치
            inventory.setItem(OUTPUT_SLOT, result);

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            player.sendMessage("§a✨ 감정 완료! 결과물을 확인하세요.");

        } else {
            player.sendMessage("§c이 아이템은 감정할 수 없습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    /**
     * 감정 결과물 생성 (랜덤)
     */
    private ItemStack generateIdentificationResult() {
        double random = Math.random();

        if (random < 0.05) {
            // 5%: 드림스톤
            ItemStack item = plugin.getItemManager().createItem("dreamstone", 1);
            if (item != null)
                return item;
        } else if (random < 0.15) {
            // 10%: 다이아몬드
            return new ItemStack(Material.DIAMOND, 1);
        } else if (random < 0.35) {
            // 20%: 금
            return new ItemStack(Material.GOLD_INGOT, 2);
        } else if (random < 0.60) {
            // 25%: 철
            return new ItemStack(Material.IRON_INGOT, 3);
        } else {
            // 40%: 석탄
            return new ItemStack(Material.COAL, 5);
        }

        return new ItemStack(Material.COBBLESTONE, 1);
    }

    @Override
    public boolean allowItemMovement() {
        return true;
    }

    @Override
    public boolean isInputSlot(int slot) {
        return slot == INPUT_SLOT || slot == OUTPUT_SLOT;
    }

    // ==================== 아이템 생성 메서드 ====================

    private ItemStack createProcessButton() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a[ ⚒ 감정하기 ]");

        List<String> lore = new ArrayList<>();
        lore.add("§7미지의 광석을 감정합니다.");
        lore.add("§7");
        lore.add("§e비용: §f10D");
        lore.add("§7");
        lore.add("§e클릭하여 실행");

        meta.setLore(lore);
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

    private ItemStack createIdentifyTab() {
        ItemStack item = new ItemStack(Material.COAL_ORE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e[ 💎 광석 감정 ]");
        meta.setLore(List.of("§7미지의 광석을 감정합니다.", "§a[현재 선택됨]"));
        item.setItemMeta(meta);

        // 글로우 효과
        item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1);
        meta = item.getItemMeta();
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createAlloyTab() {
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7[ 🔧 합금 제작 ]");
        meta.setLore(List.of("§7광물을 합금으로 제련합니다.", "§7(개발 중)"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUpgradeTab() {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7[ ⚔ 도구 강화 ]");
        meta.setLore(List.of("§7도구를 강화합니다.", "§7(개발 중)"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createArrowPane() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a→");
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
