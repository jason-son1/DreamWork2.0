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

    public FisherListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        this.fishTypeKey = new NamespacedKey(plugin, "fish_type");
    }

    /**
     * 낚시 이벤트 처리
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        // Towny 지역 확인
        if (plugin.getTownyHook().isEnabled()) {
            // 어부는 '자신의 타운' 제약이 있는지 기획서 확인 필요.
            // 보통 남의 타운에서 낚시 금지 or 보상 없음.
            // 여기서는 "타운 내부가 아니면 특수 작물이... 스킬 발동 불가" 일반 규칙 적용.
            if (plugin.getTownyHook().getTownName(player.getLocation()) != null &&
                    !plugin.getTownyHook().isInOwnTown(player, player.getLocation())) {
                return;
            }
        }

        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.FISHER);

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            handleCatch(event, player, level);
        } else if (event.getState() == PlayerFishEvent.State.FISHING) {
            // Sensitivity Logic: 입질 시간 단축
            org.bukkit.entity.FishHook hook = event.getHook();

            double reduction = 0;
            if (level >= 50)
                reduction = 0.30;
            else if (level >= 30)
                reduction = 0.15;
            else if (level >= 10)
                reduction = 0.05;

            // 미끼 보너스 (worm = 10% 추가 단축)
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if ("worm".equals(getBaitType(offhand))) {
                reduction += 0.10;
            }

            if (reduction > 0) {
                int minWait = (int) (100 * (1.0 - reduction));
                int maxWait = (int) (600 * (1.0 - reduction));
                hook.setWaitTime(Math.max(20, minWait), Math.max(100, maxWait));

                if (random.nextDouble() < 0.2) {
                    player.sendMessage("§b[어부] §f예민한 감각으로 물고기를 유인합니다. (입질 시간 " + (int) (reduction * 100) + "% 단축)");
                }
            }
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

        // 2. 커스텀 물고기 (전설 어종 등)
        boolean customCaught = false;
        if ("krill".equals(baitType)) {
            customCaught = tryCatchCustomFish(caughtItem, config, player);
        }

        // 3. 일반 물고기도 '살아있는 물고기'로 변환 (사이즈/품질 시스템 적용)
        if (!customCaught) {
            convertToLiveFish(caughtItem, config);
        }

        // 4. 보상 처리
        processRewards(player, caughtItem.getItemStack(), level, config, baitType);

        // 5. 미끼 소모
        if (baitType != null) {
            boolean save = level >= 50 && random.nextDouble() < 0.20;
            if (!save) {
                offhand.setAmount(offhand.getAmount() - 1);
            }
        }

        // 6. 내구도 보존
        if (level >= 50 && random.nextDouble() < 0.20) {
            // Logic for durability save
        }
    }

    private boolean tryCatchCustomFish(Item caughtEntity, FileConfiguration config, Player player) {
        // 미끼 보너스 확인
        ItemStack offhand = player.getInventory().getItemInOffHand();
        String bait = getBaitType(offhand);

        double chance = 0.05; // 기본 5%
        if ("krill".equals(bait)) {
            chance = 0.15; // 크릴 사용 시 15%
        } else if ("shiny_lure".equals(bait)) {
            chance = 0.10; // 루어 사용 시 10%
        }

        if (random.nextDouble() < chance) {
            plugin.debug("Custom Fish Catch Triggered for " + player.getName());
            boolean tuna = random.nextBoolean();
            String key = tuna ? "tuna" : "king_salmon";

            Material mat = Material.matchMaterial(config.getString("fish_table." + key + ".base_material", "COD"));
            ItemStack customFish = new ItemStack(mat != null ? mat : Material.COD);

            // ItemManager로 데이터 설정 (크기 등)
            double minSize = config.getDouble("fish_table." + key + ".min_size", 50.0);
            double maxSize = config.getDouble("fish_table." + key + ".max_size", 100.0);
            double size = minSize + (random.nextDouble() * (maxSize - minSize));

            // 기본 이름 설정 (ItemManager가 덮어쓸 수도 있지만, DisplayName이 필요)
            ItemMeta meta = customFish.getItemMeta();
            String display = config.getString("fish_table." + key + ".display_name", key);
            meta.setDisplayName(display);
            customFish.setItemMeta(meta);

            // ItemManager 유틸리티 사용
            customFish = plugin.getItemManager().setFishData(customFish, size);

            // PDC 추가 설정 (물고기 타입)
            meta = customFish.getItemMeta();
            meta.getPersistentDataContainer().set(fishTypeKey, PersistentDataType.STRING, key);
            customFish.setItemMeta(meta);

            caughtEntity.setItemStack(customFish);

            if (size > maxSize * 0.9) {
                // 월척 알림
                plugin.getServer().broadcastMessage("§e[어부] §f" + player.getName() + "님이 거대한 §b" + display
                        + "§f을(를) 낚았습니다! (§a" + String.format("%.1f", size) + "cm§f)");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
            }
            return true;
        }
        return false;
    }

    private void convertToLiveFish(Item caughtEntity, FileConfiguration config) {
        ItemStack fish = caughtEntity.getItemStack();
        Material type = fish.getType();

        // 물고기 종류인지 확인
        if (!isFish(type))
            return;

        // 사이즈 생성 (일반 물고기: 10~50cm)
        double minSize = 10.0;
        double maxSize = 50.0;
        double size = minSize + (random.nextDouble() * (maxSize - minSize));

        // 품질 결정 (40cm 이상 3성, 25cm 이상 2성)
        int quality = 1;
        if (size >= 40.0)
            quality = 3;
        else if (size >= 25.0)
            quality = 2;

        // ItemManager 유틸리티 사용하여 데이터 및 로어 설정
        fish = plugin.getItemManager().setFishData(fish, size);
        plugin.getItemManager().setItemQuality(fish, quality);

        // 이름 접두사 등 추가 설정
        ItemMeta meta = fish.getItemMeta();
        String color = (quality == 3) ? "§6" : (quality == 2) ? "§a" : "§f";
        String koreanName = getKoreanFishName(type);
        meta.setDisplayName(color + koreanName + " (" + String.format("%.1f", size) + "cm)");

        // PDC 데이터 (시간, 타입, 상태)
        meta.getPersistentDataContainer().set(fishTypeKey, PersistentDataType.STRING, type.name().toLowerCase());
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "catch_time"), PersistentDataType.LONG,
                System.currentTimeMillis());
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "live_fish"), PersistentDataType.BYTE,
                (byte) 1);

        fish.setItemMeta(meta);
        caughtEntity.setItemStack(fish);
    }

    /**
     * 생선 손질 (Filleting)
     */
    @EventHandler
    public void onFillet(org.bukkit.event.player.PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        // 웅크리고 생선을 들고 우클릭 시 손질
        if (player.isSneaking() && event.getAction().name().contains("RIGHT_CLICK") && isFish(item.getType())) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "live_fish"),
                    PersistentDataType.BYTE)) {

                event.setCancelled(true);

                int quality = plugin.getItemManager().getQuality(item);
                ItemStack fillet = new ItemStack(Material.COOKED_SALMON, quality); // 품질만큼 필렛 지급
                ItemMeta fMeta = fillet.getItemMeta();
                fMeta.setDisplayName("§f생선 필렛 (" + "★".repeat(quality) + ")");
                fillet.setItemMeta(fMeta);

                item.setAmount(item.getAmount() - 1);
                player.getInventory().addItem(fillet);

                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_SHEEP_SHEAR, 1.0f, 1.2f);
                player.sendMessage("§b[어부] §f생선을 손질하여 필렛을 만들었습니다.");
            }
        }
    }

    private boolean isFish(Material mat) {
        return mat == Material.COD || mat == Material.SALMON || mat == Material.PUFFERFISH
                || mat == Material.TROPICAL_FISH;
    }

    private String getKoreanFishName(Material mat) {
        return switch (mat) {
            case COD -> "대구";
            case SALMON -> "연어";
            case PUFFERFISH -> "복어";
            case TROPICAL_FISH -> "열대어";
            default -> "물고기";
        };
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
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 낚싯대를 들고 웅크린 채 우클릭 시 '어종 지식' 발동
        if (item != null && item.getType() == org.bukkit.Material.FISHING_ROD &&
                player.isSneaking() && (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                        || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {

            int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.FISHER);
            if (level < 1)
                return;

            event.setCancelled(true); // 낚시찌 던지기 방지

            org.bukkit.block.Biome biome = player.getLocation().getBlock().getBiome();
            String biomeName = biome.getKey().getKey();

            player.sendMessage("§b[어부] §f현재 지역(§e" + biomeName + "§f)의 정보:");

            // 바이옴별 어종 안내 (간단히 구현)
            if (biomeName.contains("ocean") || biomeName.contains("beach")) {
                player.sendMessage("  §7- 주요 어종: §f대구, 연어, 복어, 열대어");
                player.sendMessage("  §7- 특이 사항: §b참다랑어§7가 발견될 확률이 높습니다. (크릴 미끼 권장)");
            } else if (biomeName.contains("river") || biomeName.contains("swamp")) {
                player.sendMessage("  §7- 주요 어종: §f연어, 대구");
                player.sendMessage("  §7- 특이 사항: §6대왕 연어§7의 서식지입니다.");
            } else {
                player.sendMessage("  §7- 주요 어종: §f대구");
                player.sendMessage("  §7- 특이 사항: 일반적인 낚시터입니다.");
            }

            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
}
