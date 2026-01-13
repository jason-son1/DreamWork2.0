package com.dreamwork.gui.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.gui.DreamGui;
import com.dreamwork.job.JobType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 주방 GUI
 * 
 * 농부가 사용하는 시설입니다.
 * - 요리 제작
 * - 작물 가공
 * - 레시피 열람
 * 
 * @author DreamWork Team
 */
public class KitchenGui extends DreamGui {

    private final DreamWorkPlugin plugin;

    // 슬롯 정의
    private static final int[] INPUT_SLOTS = { 10, 11, 12 };
    private static final int OUTPUT_SLOT = 16;
    private static final int COOK_BUTTON_SLOT = 13;

    // 현재 선택된 레시피
    private String selectedRecipe = null;

    public KitchenGui(DreamWorkPlugin plugin, Player player) {
        super(plugin, player, plugin.getConfigManager().getMessage("gui.kitchen-title"), 5);
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        // 배경 채우기
        ItemStack glass = createGlassPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 45; i++) {
            boolean isInputSlot = false;
            for (int slot : INPUT_SLOTS) {
                if (i == slot)
                    isInputSlot = true;
            }
            if (!isInputSlot && i != OUTPUT_SLOT && i != COOK_BUTTON_SLOT) {
                inventory.setItem(i, glass);
            }
        }

        // 화살표 장식
        inventory.setItem(14, createArrowPane());
        inventory.setItem(15, createArrowPane());

        // 요리 버튼
        inventory.setItem(COOK_BUTTON_SLOT, createCookButton());

        // 뒤로 가기
        inventory.setItem(36, createBackButton());

        // 레시피 탭
        inventory.setItem(38, createRecipeTab("soup", "스프", Material.MUSHROOM_STEW, true));
        inventory.setItem(39, createRecipeTab("bread", "빵", Material.BREAD, false));
        inventory.setItem(40, createRecipeTab("cake", "케이크", Material.CAKE, false));
        inventory.setItem(41, createRecipeTab("steak", "스테이크", Material.COOKED_BEEF, false));
        inventory.setItem(42, createRecipeTab("golden_carrot", "황금 당근", Material.GOLDEN_CARROT, false));

        // 도움말
        inventory.setItem(44, createHelpButton());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        // 입력/출력 슬롯은 아이템 이동 허용
        boolean isInputSlot = false;
        for (int inputSlot : INPUT_SLOTS) {
            if (slot == inputSlot)
                isInputSlot = true;
        }
        if (isInputSlot || slot == OUTPUT_SLOT) {
            return;
        }

        event.setCancelled(true);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

        switch (slot) {
            case COOK_BUTTON_SLOT -> cookItem();
            case 36 -> plugin.getGuiManager().openJobDetailGui(player, JobType.FARMER);
            case 38 -> selectRecipe("soup");
            case 39 -> selectRecipe("bread");
            case 40 -> selectRecipe("cake");
            case 41 -> selectRecipe("steak");
            case 42 -> selectRecipe("golden_carrot");
        }
    }

    /**
     * 레시피 선택
     */
    private void selectRecipe(String recipe) {
        this.selectedRecipe = recipe;
        player.sendMessage("§a" + getRecipeDisplayName(recipe) + " 레시피가 선택되었습니다.");

        // GUI 새로고침 (선택 표시)
        refresh();
    }

    /**
     * 요리 실행
     */
    private void cookItem() {
        // 입력 슬롯의 아이템 확인
        List<ItemStack> inputs = new ArrayList<>();
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                inputs.add(item);
            }
        }

        if (inputs.isEmpty()) {
            player.sendMessage("§c재료를 넣어주세요!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 레시피 검증 및 결과물 생성
        ItemStack result = processRecipe(inputs);

        if (result == null) {
            player.sendMessage("§c이 재료로는 요리를 만들 수 없습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 출력 슬롯에 결과물 배치
        ItemStack existingOutput = inventory.getItem(OUTPUT_SLOT);
        if (existingOutput != null && !existingOutput.getType().isAir()) {
            if (existingOutput.isSimilar(result)) {
                existingOutput.setAmount(existingOutput.getAmount() + result.getAmount());
            } else {
                player.sendMessage("§c출력 슬롯을 비워주세요!");
                return;
            }
        } else {
            inventory.setItem(OUTPUT_SLOT, result);
        }

        // 재료 소모
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                item.setAmount(item.getAmount() - 1);
            }
        }

        // 경험치 지급
        plugin.getJobManager().addExperience(player, JobType.FARMER, 10);

        player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.0f, 1.0f);
        player.sendMessage("§a✧ 요리 완료!");
    }

    /**
     * 레시피 처리
     */
    private ItemStack processRecipe(List<ItemStack> inputs) {
        // 간단한 레시피 시스템
        // 실제로는 facilities/kitchen.yml에서 로드해야 함

        // 버섯 스프: 버섯 + 그릇
        if (hasIngredients(inputs, Material.BROWN_MUSHROOM, Material.BOWL)) {
            return new ItemStack(Material.MUSHROOM_STEW, 1);
        }

        // 빵: 밀 3개
        int wheatCount = countMaterial(inputs, Material.WHEAT);
        if (wheatCount >= 3) {
            return new ItemStack(Material.BREAD, 1);
        }

        // 황금 당근: 당근 + 금 조각
        if (hasIngredients(inputs, Material.CARROT, Material.GOLD_NUGGET)) {
            return new ItemStack(Material.GOLDEN_CARROT, 1);
        }

        // 등급별 작물로 고급 요리
        for (ItemStack item : inputs) {
            String itemId = plugin.getItemManager().getDreamItemId(item);
            if (itemId != null && itemId.endsWith("_3star")) {
                // 3성 작물로 고급 요리
                return createPremiumFood(itemId);
            }
        }

        return null;
    }

    /**
     * 재료 확인
     */
    private boolean hasIngredients(List<ItemStack> inputs, Material... materials) {
        for (Material mat : materials) {
            boolean found = false;
            for (ItemStack item : inputs) {
                if (item.getType() == mat) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }
        return true;
    }

    /**
     * 재료 개수 확인
     */
    private int countMaterial(List<ItemStack> inputs, Material material) {
        int count = 0;
        for (ItemStack item : inputs) {
            if (item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * 프리미엄 음식 생성
     */
    private ItemStack createPremiumFood(String cropId) {
        ItemStack food = new ItemStack(Material.GOLDEN_APPLE, 1);
        ItemMeta meta = food.getItemMeta();
        meta.setDisplayName("§e★ 고급 요리");
        meta.setLore(List.of(
                "§73성 작물로 만든 특별한 요리",
                "§a+8 포만감",
                "§b재생 효과 10초"));
        food.setItemMeta(meta);
        return food;
    }

    /**
     * 레시피 표시명
     */
    private String getRecipeDisplayName(String recipe) {
        return switch (recipe) {
            case "soup" -> "스프";
            case "bread" -> "빵";
            case "cake" -> "케이크";
            case "steak" -> "스테이크";
            case "golden_carrot" -> "황금 당근";
            default -> recipe;
        };
    }

    @Override
    public boolean allowItemMovement() {
        return true;
    }

    @Override
    public boolean isInputSlot(int slot) {
        for (int inputSlot : INPUT_SLOTS) {
            if (slot == inputSlot)
                return true;
        }
        return slot == OUTPUT_SLOT;
    }

    // ==================== 아이템 생성 메서드 ====================

    private ItemStack createCookButton() {
        ItemStack item = new ItemStack(Material.CAMPFIRE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a[ 🔥 요리하기 ]");

        List<String> lore = new ArrayList<>();
        lore.add("§7재료를 넣고 요리합니다.");
        lore.add("§7");
        lore.add("§e클릭하여 실행");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRecipeTab(String id, String name, Material icon, boolean selected) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        if (selected) {
            meta.setDisplayName("§a[ " + name + " ]");
            meta.setLore(List.of("§a[현재 선택됨]"));
            item.setItemMeta(meta);
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1);
            meta = item.getItemMeta();
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.setDisplayName("§7[ " + name + " ]");
            meta.setLore(List.of("§7클릭하여 선택"));
        }

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
                "§7주방 사용법:",
                "§71. 재료를 왼쪽 슬롯에 넣습니다.",
                "§72. 요리하기 버튼을 클릭합니다.",
                "§73. 결과물을 수거합니다."));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createArrowPane() {
        ItemStack item = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6→");
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
