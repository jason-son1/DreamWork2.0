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
}
