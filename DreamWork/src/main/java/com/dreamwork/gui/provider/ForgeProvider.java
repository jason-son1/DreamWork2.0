package com.dreamwork.gui.provider;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 대장간 기능 제공자
 * 
 * 대장간의 감정, 합금, 강화 로직을 처리합니다.
 * 
 * @author DreamWork Team
 */
public class ForgeProvider {

    private final DreamWorkPlugin plugin;
    private final Random random = new Random();

    public ForgeProvider(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 감정 비용 가져오기
     */
    public double getIdentifyCost() {
        FileConfiguration config = plugin.getConfigManager().getJobConfig("miner");
        if (config == null)
            return 10.0;
        return config.getDouble("forge.identify.cost", 10.0);
    }

    /**
     * 감정 결과 생성
     */
    public ItemStack identifyItem() {
        FileConfiguration config = plugin.getConfigManager().getJobConfig("miner");
        if (config == null)
            return new ItemStack(Material.COBBLESTONE);

        ConfigurationSection results = config.getConfigurationSection("forge.identify.results");
        if (results == null)
            return new ItemStack(Material.COBBLESTONE);

        double roll = random.nextDouble();
        double currentChance = 0.0;

        for (String key : results.getKeys(false)) {
            ConfigurationSection section = results.getConfigurationSection(key);
            if (section == null)
                continue;

            double chance = section.getDouble("chance", 0.0);
            currentChance += chance;

            if (roll <= currentChance) {
                // 커스텀 아이템
                String itemId = section.getString("item");
                int amount = section.getInt("amount", 1);

                if (itemId != null) {
                    ItemStack item = plugin.getItemManager().createItem(itemId, amount);
                    if (item != null)
                        return item;
                }

                // 바닐라 아이템
                String materialName = section.getString("material");
                if (materialName != null) {
                    Material material = Material.matchMaterial(materialName);
                    if (material != null) {
                        return new ItemStack(material, amount);
                    }
                }
            }
        }

        // 기본 꽝 (돌)
        return new ItemStack(Material.STONE, 1);
    }

    /**
     * 미감정 아이템인지 확인
     */
    public boolean isUnidentified(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;

        // 1. ID 체크 (하위 호환)
        String id = plugin.getItemManager().getDreamItemId(item);
        if ("unknown_ore".equals(id))
            return true;

        // 2. PDC 체크
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "dw_unidentified");
        return item.getItemMeta().getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE);
    }

    /**
     * 합금 제작 시도
     * 
     * @param input1 첫 번째 재료
     * @param input2 두 번째 재료
     * @return 결과물 (성공/실패 포함), 레시피 없으면 null
     */
    public ItemStack tryCreateAlloy(ItemStack input1, ItemStack input2) {
        if (input1 == null || input2 == null)
            return null;

        FileConfiguration config = plugin.getConfigManager().getJobConfig("miner");
        if (config == null)
            return null;

        ConfigurationSection alloys = config.getConfigurationSection("forge.alloys");
        if (alloys == null)
            return null;

        for (String key : alloys.getKeys(false)) {
            ConfigurationSection recipe = alloys.getConfigurationSection(key + ".recipe");
            if (recipe == null)
                continue;

            // 재료 확인 (순서 무관 체크)
            if (matchRecipe(recipe, input1, input2)) {

                // 확률 체크
                double failChance = alloys.getDouble(key + ".failure_chance", 0.0);
                if (failChance > 0 && random.nextDouble() < failChance) {
                    // 실패 처리
                    String failResultId = alloys.getString(key + ".failure_result");
                    if (failResultId != null) {
                        return plugin.getItemManager().createItem(failResultId, 1);
                    }
                    // 기본 실패 부산물: 석탄
                    return new ItemStack(Material.COAL);
                }

                // 성공
                String resultId = alloys.getString(key + ".result");
                int amount = alloys.getInt(key + ".amount", 1);

                if (resultId != null) {
                    return plugin.getItemManager().createItem(resultId, amount);
                }
            }
        }
        return null;
    }

    private boolean matchRecipe(ConfigurationSection recipe, ItemStack i1, ItemStack i2) {
        String mat1 = recipe.getString("input1");
        String mat2 = recipe.getString("input2");

        if (mat1 == null || mat2 == null)
            return false;

        // Case 1: i1=mat1, i2=mat2
        if (checkItem(i1, mat1) && checkItem(i2, mat2))
            return true;
        // Case 2: i1=mat2, i2=mat1
        if (checkItem(i1, mat2) && checkItem(i2, mat1))
            return true;

        return false;
    }

    private boolean checkItem(ItemStack item, String idOrMaterial) {
        // 드림 아이템 ID 먼저 체크
        if (plugin.getItemManager().isItem(item, idOrMaterial))
            return true;
        // 바닐라 Material 체크
        try {
            Material m = Material.matchMaterial(idOrMaterial);
            if (m != null && item.getType() == m)
                return true;
        } catch (Exception e) {
        }
        return false;
    }
}
