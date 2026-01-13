package com.dreamwork.gui.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.gui.DreamGui;
import com.dreamwork.job.JobType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 어시장 GUI
 * 
 * 어부가 사용하는 시설입니다.
 * - 물고기 납품 (판매)
 * - 살아있는 물고기 보관
 * - 대어 등록
 * 
 * @author DreamWork Team
 */
public class FishMarketGui extends DreamGui {

    private final DreamWorkPlugin plugin;

    // 슬롯 정의
    private static final int[] FISH_SLOTS = { 10, 11, 12, 13, 14, 15, 16 };
    private static final int SELL_ALL_BUTTON = 31;

    // 판매 가격 배율
    private double priceMultiplier = 1.0;

    public FishMarketGui(DreamWorkPlugin plugin, Player player) {
        super(plugin, player, plugin.getConfigManager().getMessage("gui.fishmarket-title"), 5);
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        // 배경 채우기
        ItemStack glass = createGlassPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        for (int i = 0; i < 45; i++) {
            boolean isFishSlot = false;
            for (int slot : FISH_SLOTS) {
                if (i == slot)
                    isFishSlot = true;
            }
            if (!isFishSlot && i != SELL_ALL_BUTTON) {
                inventory.setItem(i, glass);
            }
        }

        // 상단 장식
        inventory.setItem(4, createMarketTitle());

        // 납품 슬롯 표시
        for (int slot : FISH_SLOTS) {
            // 빈 슬롯 표시 (물고기 아이콘)
            inventory.setItem(slot, createEmptyFishSlot());
        }

        // 전체 판매 버튼
        inventory.setItem(SELL_ALL_BUTTON, createSellAllButton());

        // 뒤로 가기
        inventory.setItem(36, createBackButton());

        // 가격표
        inventory.setItem(38, createPriceList());

        // 대어 기록
        inventory.setItem(40, createRecordBoard());

        // 보너스 정보
        inventory.setItem(42, createBonusInfo());

        // 도움말
        inventory.setItem(44, createHelpButton());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        // 물고기 슬롯은 아이템 이동 허용
        boolean isFishSlot = false;
        for (int fishSlot : FISH_SLOTS) {
            if (slot == fishSlot)
                isFishSlot = true;
        }
        if (isFishSlot) {
            return;
        }

        event.setCancelled(true);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

        switch (slot) {
            case SELL_ALL_BUTTON -> sellAllFish();
            case 36 -> plugin.getGuiManager().openJobDetailGui(player, JobType.FISHER);
        }
    }

    /**
     * 모든 물고기 판매
     */
    private void sellAllFish() {
        double totalEarnings = 0;
        int fishCount = 0;

        for (int slot : FISH_SLOTS) {
            ItemStack item = inventory.getItem(slot);

            if (item == null || item.getType().isAir())
                continue;
            if (item.getType() == Material.TROPICAL_FISH_BUCKET)
                continue; // 빈 슬롯 아이콘

            // 물고기인지 확인
            double price = getFishPrice(item);
            if (price <= 0)
                continue;

            // 살아있는 물고기 보너스
            String itemId = plugin.getItemManager().getDreamItemId(item);
            if (itemId != null && itemId.contains("_fresh")) {
                price *= 1.5; // 50% 보너스
            }

            double earnings = price * item.getAmount() * priceMultiplier;
            totalEarnings += earnings;
            fishCount += item.getAmount();

            // 슬롯 비우기
            inventory.setItem(slot, createEmptyFishSlot());
        }

        if (fishCount == 0) {
            player.sendMessage("§c판매할 물고기가 없습니다!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 돈 지급
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            plugin.getVaultHook().deposit(player, totalEarnings);
        }

        // 경험치 지급
        double exp = fishCount * 5.0;
        plugin.getJobManager().addExperience(player, JobType.FISHER, exp);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendMessage(String.format("§a🐟 물고기 %d마리를 판매하여 §e%.0fD§a를 획득했습니다!",
                fishCount, totalEarnings));
    }

    /**
     * 물고기 가격 조회
     */
    private double getFishPrice(ItemStack item) {
        Material type = item.getType();

        // 기본 물고기 가격
        return switch (type) {
            case COD -> 5.0;
            case SALMON -> 8.0;
            case TROPICAL_FISH -> 15.0;
            case PUFFERFISH -> 20.0;
            default -> 0.0;
        };
    }

    @Override
    public boolean allowItemMovement() {
        return true;
    }

    @Override
    public boolean isInputSlot(int slot) {
        for (int fishSlot : FISH_SLOTS) {
            if (slot == fishSlot)
                return true;
        }
        return false;
    }

    // ==================== 아이템 생성 메서드 ====================

    private ItemStack createMarketTitle() {
        ItemStack item = new ItemStack(Material.AXOLOTL_BUCKET);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§l🐟 어시장 §7- 물고기 납품");
        meta.setLore(List.of(
                "§7물고기를 아래 슬롯에 넣고",
                "§7판매 버튼을 눌러주세요."));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEmptyFishSlot() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7[ 물고기 넣기 ]");
        meta.setLore(List.of("§7여기에 물고기를 넣으세요."));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSellAllButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e[ 💰 전체 판매 ]");

        List<String> lore = new ArrayList<>();
        lore.add("§7위 슬롯의 모든 물고기를 판매합니다.");
        lore.add("§7");
        lore.add("§a살아있는 물고기: §f+50% 가격");
        lore.add("§7");
        lore.add("§e클릭하여 판매");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPriceList() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e[ 📋 가격표 ]");

        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§f대구: §e5D");
        lore.add("§f연어: §e8D");
        lore.add("§f열대어: §e15D");
        lore.add("§f복어: §e20D");
        lore.add("§7");
        lore.add("§a살아있는 물고기 +50%");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRecordBoard() {
        ItemStack item = new ItemStack(Material.OAK_HANGING_SIGN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6[ 🏆 대어 기록 ]");

        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§f🥇 1위: §e???cm §7- ???");
        lore.add("§f🥈 2위: §e???cm §7- ???");
        lore.add("§f🥉 3위: §e???cm §7- ???");
        lore.add("§7");
        lore.add("§7(추후 구현)");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBonusInfo() {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b[ ✨ 보너스 정보 ]");

        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§f현재 가격 배율: §a" + String.format("%.1fx", priceMultiplier));
        lore.add("§7");
        lore.add("§7타운 등급에 따라 보너스가 적용됩니다.");

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

    private ItemStack createHelpButton() {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e[ ? 도움말 ]");
        meta.setLore(List.of(
                "§7어시장 사용법:",
                "§71. 물고기를 슬롯에 넣습니다.",
                "§72. 전체 판매 버튼을 클릭합니다.",
                "§73. 돈을 받습니다!",
                "§7",
                "§aTIP: 살아있는 물고기는 더 비쌉니다!"));
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
