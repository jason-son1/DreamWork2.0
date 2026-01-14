package com.dreamwork.shop.injector;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

/**
 * 상점 주입기 (Shop Injector)
 * 
 * DreamWork에서 정의한 상점 설정을 EconomyShop API를 통해 주입합니다.
 * 리플렉션을 사용하여 EconomyShop 버전 호환성을 유지합니다.
 * 
 * @author DreamWork Team
 */
public class ShopInjector {

    private final DreamWorkPlugin plugin;
    private final ItemConverter itemConverter;
    private final Set<String> injectedSectionIds;
    private final Map<String, DreamShopConfig> loadedConfigs;

    public ShopInjector(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        this.itemConverter = new ItemConverter(plugin);
        this.injectedSectionIds = new HashSet<>();
        this.loadedConfigs = new HashMap<>();
    }

    /**
     * 모든 상점 설정을 로드하고 EconomyShop에 주입
     */
    public void injectAllShops() {
        plugin.log(Level.INFO, "DreamWork 상점을 EconomyShop에 주입하는 중...");

        loadAllShopConfigs();

        if (loadedConfigs.isEmpty()) {
            plugin.log(Level.INFO, "주입할 상점이 없습니다.");
            return;
        }

        int successCount = 0;
        for (DreamShopConfig config : loadedConfigs.values()) {
            if (injectShop(config)) {
                successCount++;
            }
        }

        plugin.log(Level.INFO, "DreamWork 상점 주입 완료! (" + successCount + "/" + loadedConfigs.size() + " 개)");
    }

    /**
     * shops/ 폴더에서 모든 상점 설정 로드
     */
    private void loadAllShopConfigs() {
        loadedConfigs.clear();

        File shopsFolder = new File(plugin.getDataFolder(), "shops");
        if (!shopsFolder.exists()) {
            saveDefaultShopConfigs();
        }

        File[] files = shopsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.debug("shops 폴더에 설정 파일이 없습니다.");
            return;
        }

        for (File file : files) {
            try {
                DreamShopConfig config = loadShopConfig(file);
                if (config != null && config.getSectionId() != null) {
                    loadedConfigs.put(config.getSectionId(), config);
                    plugin.debug("상점 설정 로드됨: " + config.getSectionId());
                }
            } catch (Exception e) {
                plugin.log(Level.WARNING, "상점 설정 로드 실패: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * 기본 상점 설정 파일 저장
     */
    private void saveDefaultShopConfigs() {
        String[] defaults = { "miner_shop.yml", "farmer_shop.yml", "fisher_shop.yml", "hunter_shop.yml",
                "adventurer_shop.yml" };
        for (String fileName : defaults) {
            try {
                plugin.saveResource("shops/" + fileName, false);
            } catch (Exception e) {
                plugin.debug("기본 상점 설정 저장 실패: " + fileName);
            }
        }
    }

    /**
     * 단일 YAML 파일에서 상점 설정 로드
     */
    private DreamShopConfig loadShopConfig(File file) {
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        DreamShopConfig config = new DreamShopConfig();
        config.setSectionId(yaml.getString("section_id"));
        config.setDisplayName(yaml.getString("display_name", "§f상점"));
        config.setDescription(yaml.getString("description", ""));

        String iconStr = yaml.getString("icon", "CHEST");
        try {
            config.setIcon(Material.valueOf(iconStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            config.setIcon(Material.CHEST);
        }

        config.setPermission(yaml.getString("permission", ""));
        config.setEconomy(yaml.getString("economy", "Vault"));
        config.setRows(yaml.getInt("rows", 6));

        // 경험치 보너스 설정
        ConfigurationSection expSection = yaml.getConfigurationSection("job_exp_bonus");
        if (expSection != null) {
            config.setJobExpEnabled(expSection.getBoolean("enabled", true));
            config.setBaseExpPerSale(expSection.getDouble("base_exp_per_sale", 1.0));
            config.setPriceExpRatio(expSection.getDouble("price_exp_ratio", 0.01));
        }

        // 아이템 목록 로드
        if (yaml.isList("items")) {
            List<Map<?, ?>> itemsList = yaml.getMapList("items");
            for (Map<?, ?> itemMap : itemsList) {
                DreamShopItemConfig itemConfig = parseItemConfig(itemMap);
                if (itemConfig != null) {
                    config.addItem(itemConfig);
                }
            }
        }

        return config;
    }

    /**
     * Map에서 아이템 설정 파싱
     */
    private DreamShopItemConfig parseItemConfig(Map<?, ?> map) {
        DreamShopItemConfig config = new DreamShopItemConfig();

        config.setId(getString(map, "id"));

        String materialStr = getString(map, "material");
        if (materialStr != null) {
            try {
                config.setMaterial(Material.valueOf(materialStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                config.setMaterial(Material.STONE);
            }
        }

        config.setDreamworkItemId(getString(map, "dreamwork_item"));
        config.setName(getString(map, "name"));

        Object loreObj = map.get("lore");
        if (loreObj instanceof List) {
            List<String> loreList = new ArrayList<>();
            for (Object line : (List<?>) loreObj) {
                loreList.add(String.valueOf(line));
            }
            config.setLore(loreList);
        }

        config.setSlot(getInt(map, "slot", 0));
        config.setBuyPrice(getDouble(map, "buy_price", -1));
        config.setSellPrice(getDouble(map, "sell_price", -1));
        config.setDynamicPricing(getBoolean(map, "dynamic_pricing", false));
        config.setMaxStock(getInt(map, "max_stock", 0));

        return config;
    }

    /**
     * 단일 상점을 EconomyShop에 주입 (리플렉션 사용)
     */
    public boolean injectShop(DreamShopConfig config) {
        try {
            // EconomyShop 인스턴스 가져오기
            Class<?> esClass = Class.forName("me.antigravity.economyshop.EconomyShop");
            Object economyShop = esClass.getMethod("getInstance").invoke(null);
            if (economyShop == null) {
                plugin.log(Level.WARNING, "EconomyShop 인스턴스를 가져올 수 없습니다.");
                return false;
            }

            // 이미 등록된 상점이면 스킵
            if (injectedSectionIds.contains(config.getSectionId())) {
                plugin.debug("이미 주입된 상점: " + config.getSectionId());
                return true;
            }

            // API 가져오기
            Object api = esClass.getMethod("getApi").invoke(economyShop);
            if (api == null) {
                plugin.log(Level.WARNING, "EconomyShop API를 가져올 수 없습니다.");
                return false;
            }

            // ShopBuilder 생성
            Method createBuilderMethod = api.getClass().getMethod("createShopBuilder");
            Object builder = createBuilderMethod.invoke(api);
            Class<?> builderClass = builder.getClass();

            // 빌더 설정
            String displayName = ChatColor.translateAlternateColorCodes('&', config.getDisplayName());
            builder = builderClass.getMethod("id", String.class).invoke(builder, config.getSectionId());
            builder = builderClass.getMethod("displayName", String.class).invoke(builder, displayName);
            builder = builderClass.getMethod("icon", Material.class).invoke(builder, config.getIcon());

            // 권한 설정
            if (config.getPermission() != null && !config.getPermission().isEmpty()) {
                try {
                    builder = builderClass.getMethod("permission", String.class).invoke(builder,
                            config.getPermission());
                } catch (NoSuchMethodException e) {
                    plugin.debug("permission 메서드 없음");
                }
            }

            // 경제 설정
            if (config.getEconomy() != null) {
                try {
                    builder = builderClass.getMethod("economy", String.class).invoke(builder, config.getEconomy());
                } catch (NoSuchMethodException e) {
                    plugin.debug("economy 메서드 없음");
                }
            }

            // 빌드
            Method buildMethod = builderClass.getMethod("build");
            Object section = buildMethod.invoke(builder);

            if (section != null) {
                // 아이템 추가
                Class<?> sectionClass = section.getClass();
                for (DreamShopItemConfig itemConfig : config.getItems()) {
                    try {
                        Object shopItem = itemConverter.convert(itemConfig);
                        if (shopItem != null) {
                            // addItem 또는 다른 메서드로 아이템 추가
                            try {
                                sectionClass.getMethod("addItem", shopItem.getClass()).invoke(section, shopItem);
                            } catch (NoSuchMethodException e) {
                                // 다른 방법 시도
                                try {
                                    Method getItemsMethod = sectionClass.getMethod("getItems");
                                    Object items = getItemsMethod.invoke(section);
                                    if (items instanceof List) {
                                        ((List<Object>) items).add(shopItem);
                                    }
                                } catch (Exception ex) {
                                    plugin.debug("아이템 추가 방법을 찾을 수 없음");
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.debug("아이템 추가 실패: " + itemConfig.getId() + " - " + e.getMessage());
                    }
                }

                injectedSectionIds.add(config.getSectionId());
                plugin.debug("상점 주입 성공: " + config.getSectionId() + " (" + config.getItems().size() + " 아이템)");
                return true;
            }

            return false;

        } catch (Exception e) {
            plugin.log(Level.WARNING, "상점 주입 실패: " + config.getSectionId() + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 모든 주입된 상점 제거 (리로드용)
     */
    public void unloadAllShops() {
        if (injectedSectionIds.isEmpty()) {
            return;
        }

        plugin.debug("주입된 상점 제거 중...");

        try {
            Class<?> esClass = Class.forName("me.antigravity.economyshop.EconomyShop");
            Object economyShop = esClass.getMethod("getInstance").invoke(null);

            if (economyShop != null) {
                Object shopManager = esClass.getMethod("getShopManager").invoke(economyShop);
                if (shopManager != null) {
                    Class<?> smClass = shopManager.getClass();

                    for (String sectionId : injectedSectionIds) {
                        try {
                            // unregisterSection 시도
                            try {
                                smClass.getMethod("unregisterSection", String.class).invoke(shopManager, sectionId);
                            } catch (NoSuchMethodException e) {
                                // removeSection 시도
                                try {
                                    smClass.getMethod("removeSection", String.class).invoke(shopManager, sectionId);
                                } catch (NoSuchMethodException e2) {
                                    plugin.debug("상점 제거 메서드를 찾을 수 없음: " + sectionId);
                                }
                            }
                            plugin.debug("상점 제거됨: " + sectionId);
                        } catch (Exception e) {
                            plugin.debug("상점 제거 실패: " + sectionId + " - " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.debug("상점 언로드 중 오류: " + e.getMessage());
        }

        injectedSectionIds.clear();
        loadedConfigs.clear();
    }

    /**
     * 특정 상점 설정 가져오기
     */
    public DreamShopConfig getShopConfig(String sectionId) {
        return loadedConfigs.get(sectionId);
    }

    /**
     * 주입된 상점 ID 목록
     */
    public Set<String> getInjectedSectionIds() {
        return Collections.unmodifiableSet(injectedSectionIds);
    }

    // 유틸리티 메서드
    private String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private int getInt(Map<?, ?> map, String key, int def) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return def;
    }

    private double getDouble(Map<?, ?> map, String key, double def) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return def;
    }

    private boolean getBoolean(Map<?, ?> map, String key, boolean def) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return def;
    }
}
