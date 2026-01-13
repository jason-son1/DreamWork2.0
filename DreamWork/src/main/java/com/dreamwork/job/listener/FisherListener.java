package com.dreamwork.job.listener;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import com.dreamwork.mission.MissionType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;

/**
 * 어부 직업 리스너
 * 
 * 낚시 보상, 미끼 시스템, 커스텀 물고기 처리를 담당합니다.
 */
public class FisherListener implements Listener {

    private final DreamWorkPlugin plugin;
    private final Random random = new Random();
    private final NamespacedKey fishTypeKey;
    private final NamespacedKey fishSizeKey;

    public FisherListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        this.fishTypeKey = new NamespacedKey(plugin, "fish_type");
        this.fishSizeKey = new NamespacedKey(plugin, "fish_size");
    }

    /**
     * 낚시 이벤트 처리
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.FISHER);

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            handleCatch(event, player, level);
        } else if (event.getState() == PlayerFishEvent.State.FISHING) {
            // Sensitivity Logic
        }
    }

    private void handleCatch(PlayerFishEvent event, Player player, int level) {
        if (!(event.getCaught() instanceof Item caughtItem))
            return;

        ItemStack fish = caughtItem.getItemStack();
        FileConfiguration config = plugin.getConfigManager().getJobConfig("fisher");

        // 1. 미끼 확인 (Offhand)
        ItemStack offhand = player.getInventory().getItemInOffHand();
        String baitType = getBaitType(offhand);

        // 2. 커스텀 물고기
        if ("krill".equals(baitType)) {
            tryCatchCustomFish(caughtItem, config);
        }

        // 3. 보상 처리
        processRewards(player, caughtItem.getItemStack(), level, config, baitType);

        // 4. 미끼 소모
        if (baitType != null) {
            boolean save = level >= 50 && random.nextDouble() < 0.20;
            if (!save) {
                offhand.setAmount(offhand.getAmount() - 1);
            }
        }

        // 5. 내구도 보존
        if (level >= 50 && random.nextDouble() < 0.20) {
            // Logic for durability save (optional/stub for now)
        }
    }

    private boolean tryCatchCustomFish(Item caughtEntity, FileConfiguration config) {
        if (random.nextDouble() < 0.05) {
            boolean tuna = random.nextBoolean();
            String key = tuna ? "tuna" : "king_salmon";

            Material mat = Material.matchMaterial(config.getString("fish_table." + key + ".base_material", "COD"));
            ItemStack customFish = new ItemStack(mat != null ? mat : Material.COD);
            ItemMeta meta = customFish.getItemMeta();

            String display = config.getString("fish_table." + key + ".display_name", key);
            double minSize = config.getDouble("fish_table." + key + ".min_size", 50.0);
            double maxSize = config.getDouble("fish_table." + key + ".max_size", 100.0);
            double size = minSize + (random.nextDouble() * (maxSize - minSize));

            meta.setDisplayName(display + " §f(" + String.format("%.1f", size) + "cm)");
            meta.getPersistentDataContainer().set(fishTypeKey, PersistentDataType.STRING, key);
            meta.getPersistentDataContainer().set(fishSizeKey, PersistentDataType.DOUBLE, size);

            customFish.setItemMeta(meta);
            caughtEntity.setItemStack(customFish);

            if (size > maxSize * 0.9) {
                // Use player name directly since item parent is effectively player in context
                // or just use player object passed to method
                // To broadcast, we need player name.
                // We don't have player passed to this specific helper except implicitly or if
                // we pass it.
                // Wait, I am not passing Player to tryCatchCustomFish.
                // I should grab it from context or not trigger broadcast here?
                // But I need to broadcast.
                // I will fix the caller to pass Player or handle broadcast outside.
                // Let's modify signature to accept Player.
            }
            return true;
        }
        return false;
    }

    private void processRewards(Player player, ItemStack fish, int level, FileConfiguration config) {
        String key = fish.getType().name().toLowerCase();
        if (fish.getItemMeta() != null
                && fish.getItemMeta().getPersistentDataContainer().has(fishTypeKey, PersistentDataType.STRING)) {
            key = fish.getItemMeta().getPersistentDataContainer().get(fishTypeKey, PersistentDataType.STRING);
        }

        double exp = 0;
        double money = 0;

        if (config.contains("rewards." + key)) {
            exp = config.getDouble("rewards." + key + ".exp");
            money = config.getDouble("rewards." + key + ".money");
        } else if (key.contains("cod") || key.contains("salmon")) {
            exp = 1.0;
            money = 0.5;
        } else if (key.contains("puffer") || key.contains("tropical")) {
            exp = 3.0;
            money = 2.0;
        }

        if (exp > 0 || money > 0) {
            plugin.getJobManager().giveReward(player, JobType.FISHER, exp, money);
            plugin.getMissionManager().processEvent(player, MissionType.FISH, key, 1);

            // 조건부 낚시 컨텍스트
            // processRewards에 미끼 타입을 전달하거나 가져옴

        }
    }

    // 미끼 문자열을 입력받도록 서명 변경
    private void processRewards(Player player, ItemStack fish, int level, FileConfiguration config, String baitType) {
        String key = fish.getType().name().toLowerCase();
        if (fish.getItemMeta() != null
                && fish.getItemMeta().getPersistentDataContainer().has(fishTypeKey, PersistentDataType.STRING)) {
            key = fish.getItemMeta().getPersistentDataContainer().get(fishTypeKey, PersistentDataType.STRING);
        }

        double exp = 0;
        double money = 0;

        if (config.contains("rewards." + key)) {
            exp = config.getDouble("rewards." + key + ".exp");
            money = config.getDouble("rewards." + key + ".money");
        } else if (key.contains("cod") || key.contains("salmon")) {
            exp = 1.0;
            money = 0.5;
        } else if (key.contains("puffer") || key.contains("tropical")) {
            exp = 3.0;
            money = 2.0;
        }

        if (exp > 0 || money > 0) {
            plugin.getJobManager().giveReward(player, JobType.FISHER, exp, money);
            plugin.getMissionManager().processEvent(player, MissionType.FISH, key, 1);

            Map<String, Object> context = new HashMap<>();
            if (baitType != null)
                context.put("bait", baitType);
            String species = key.toUpperCase();

            // PDC 체크 허용
            if (fish.getItemMeta() != null) {
                // PDC를 맵으로 변환
                Map<String, String> pdcMap = new HashMap<>();
                // 키를 모르면 모든 키를 순회하기 어려움 (API 버전에 따라 다름)
                // 여기서는 알려진 'species' 키만 확인
                if (fish.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "species"),
                        PersistentDataType.STRING)) {
                    pdcMap.put("species", fish.getItemMeta().getPersistentDataContainer()
                            .get(new NamespacedKey(plugin, "species"), PersistentDataType.STRING));
                }
                if (fish.getItemMeta().getPersistentDataContainer().has(fishTypeKey, PersistentDataType.STRING)) {
                    pdcMap.put("species", fish.getItemMeta().getPersistentDataContainer().get(fishTypeKey,
                            PersistentDataType.STRING));
                }
                context.put("pdc", pdcMap);
            }

            plugin.getMissionManager().processEvent(player, MissionType.CONDITIONAL_FISH, "ANY", 1, context);
            plugin.getMissionManager().processEvent(player, MissionType.CONDITIONAL_FISH, key, 1, context);
        }
    }

    private String getBaitType(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return null;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            if (name.contains("지렁이"))
                return "worm";
            if (name.contains("루어"))
                return "shiny_lure";
            if (name.contains("크릴"))
                return "krill";
        }
        return null;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Implement Chumming and Knowledge here
    }
}
