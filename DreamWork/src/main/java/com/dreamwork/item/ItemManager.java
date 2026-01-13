package com.dreamwork.item;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.logging.Level;

/**
 * 아이템 관리자
 * 
 * YAML 설정 파일에서 커스텀 아이템을 로드하고 생성합니다.
 * PDC(PersistentDataContainer)를 사용하여 아이템에 데이터를 저장합니다.
 * 
 * @author DreamWork Team
 */
public class ItemManager {

    private final DreamWorkPlugin plugin;

    // 아이템 템플릿 캐시 (ID -> Template)
    private final Map<String, DreamItemTemplate> itemCache = new HashMap<>();

    // PDC 키
    private final NamespacedKey keyId; // dw_id
    private final NamespacedKey keyCategory; // dw_category
    private final NamespacedKey keyQuality; // dw_quality
    private final NamespacedKey keyTimestamp; // dw_timestamp

    public ItemManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;

        // NamespacedKey 초기화
        this.keyId = new NamespacedKey(plugin, "dw_id");
        this.keyCategory = new NamespacedKey(plugin, "dw_category");
        this.keyQuality = new NamespacedKey(plugin, "dw_quality");
        this.keyTimestamp = new NamespacedKey(plugin, "dw_timestamp");
    }

    /**
     * 모든 아이템 설정 로드
     */
    public void loadItems() {
        itemCache.clear();

        Map<String, FileConfiguration> itemConfigs = plugin.getConfigManager().getAllItemConfigs();

        for (Map.Entry<String, FileConfiguration> entry : itemConfigs.entrySet()) {
            String category = entry.getKey();
            FileConfiguration config = entry.getValue();

            if (config == null)
                continue;

            // items 섹션의 모든 아이템 로드
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            if (itemsSection == null) {
                // 루트에 직접 아이템이 있는 경우
                for (String key : config.getKeys(false)) {
                    if (config.isConfigurationSection(key)) {
                        loadItem(key, config.getConfigurationSection(key), category);
                    }
                }
            } else {
                for (String key : itemsSection.getKeys(false)) {
                    loadItem(key, itemsSection.getConfigurationSection(key), category);
                }
            }
        }

        plugin.log(Level.INFO, "아이템 " + itemCache.size() + "개 로드 완료");
    }

    /**
     * 개별 아이템 로드
     */
    private void loadItem(String id, ConfigurationSection section, String category) {
        if (section == null)
            return;

        try {
            DreamItemTemplate template = new DreamItemTemplate();
            template.setId(id);
            template.setCategory(category);

            // 재질
            String materialName = section.getString("material", "STONE");
            Material material = Material.getMaterial(materialName.toUpperCase());
            if (material == null) {
                plugin.log(Level.WARNING, "알 수 없는 Material: " + materialName + " (아이템: " + id + ")");
                material = Material.STONE;
            }
            template.setMaterial(material);

            // 이름
            template.setDisplayName(translateColors(section.getString("name", id)));

            // 설명 (Lore)
            List<String> lore = section.getStringList("lore");
            template.setLore(lore.stream()
                    .map(this::translateColors)
                    .toList());

            // PDC 데이터
            ConfigurationSection pdcSection = section.getConfigurationSection("pdc_data");
            if (pdcSection != null) {
                Map<String, Object> pdcData = new HashMap<>();
                for (String key : pdcSection.getKeys(false)) {
                    pdcData.put(key, pdcSection.get(key));
                }
                template.setPdcData(pdcData);
            }

            // 품질 (등급)
            template.setQuality(section.getInt("quality", 0));

            // 커스텀 모델 데이터
            template.setCustomModelData(section.getInt("custom_model_data", 0));

            // 인챈트 효과 (글로우)
            template.setGlowing(section.getBoolean("glowing", false));

            itemCache.put(id, template);
            plugin.debug("아이템 로드됨: " + id + " (" + category + ")");

        } catch (Exception e) {
            plugin.log(Level.WARNING, "아이템 로드 실패: " + id + " - " + e.getMessage());
        }
    }

    /**
     * 아이템 생성
     * 
     * @param id     아이템 ID
     * @param amount 수량
     * @return 생성된 ItemStack, 없으면 null
     */
    public ItemStack createItem(String id, int amount) {
        DreamItemTemplate template = itemCache.get(id);
        if (template == null) {
            plugin.debug("아이템 템플릿을 찾을 수 없음: " + id);
            return null;
        }

        return buildItem(template, amount, Collections.emptyMap());
    }

    /**
     * 플레이스홀더가 포함된 아이템 생성
     */
    public ItemStack createItem(String id, int amount, Map<String, String> placeholders) {
        DreamItemTemplate template = itemCache.get(id);
        if (template == null)
            return null;

        return buildItem(template, amount, placeholders);
    }

    /**
     * 템플릿으로부터 ItemStack 생성
     */
    private ItemStack buildItem(DreamItemTemplate template, int amount, Map<String, String> placeholders) {
        ItemStack item = new ItemStack(template.getMaterial(), amount);
        ItemMeta meta = item.getItemMeta();

        if (meta == null)
            return item;

        // 이름 설정
        String displayName = template.getDisplayName();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            displayName = displayName.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        meta.setDisplayName(displayName);

        // 설명 설정
        List<String> lore = new ArrayList<>();
        for (String line : template.getLore()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                line = line.replace("<" + entry.getKey() + ">", entry.getValue());
            }
            lore.add(line);
        }
        meta.setLore(lore);

        // 커스텀 모델 데이터
        if (template.getCustomModelData() > 0) {
            meta.setCustomModelData(template.getCustomModelData());
        }

        // PDC 데이터 설정
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyId, PersistentDataType.STRING, template.getId());
        pdc.set(keyCategory, PersistentDataType.STRING, template.getCategory());
        pdc.set(keyQuality, PersistentDataType.INTEGER, template.getQuality());
        pdc.set(keyTimestamp, PersistentDataType.LONG, System.currentTimeMillis());

        // 추가 PDC 데이터
        if (template.getPdcData() != null) {
            for (Map.Entry<String, Object> entry : template.getPdcData().entrySet()) {
                NamespacedKey key = new NamespacedKey(plugin, entry.getKey());
                Object value = entry.getValue();

                if (value instanceof Integer) {
                    pdc.set(key, PersistentDataType.INTEGER, (Integer) value);
                } else if (value instanceof Double) {
                    pdc.set(key, PersistentDataType.DOUBLE, (Double) value);
                } else if (value instanceof Boolean) {
                    pdc.set(key, PersistentDataType.BYTE, (byte) ((Boolean) value ? 1 : 0));
                } else {
                    pdc.set(key, PersistentDataType.STRING, String.valueOf(value));
                }
            }
        }

        // 글로우 효과
        if (template.isGlowing()) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 아이템의 DreamWork ID 확인
     * 
     * @return DreamWork ID, 바닐라 아이템이면 null
     */
    public String getDreamItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(keyId, PersistentDataType.STRING);
    }

    /**
     * 아이템의 카테고리 확인
     */
    public String getCategory(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(keyCategory, PersistentDataType.STRING);
    }

    /**
     * 아이템의 품질(등급) 확인
     */
    public int getQuality(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return 0;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(keyQuality, PersistentDataType.INTEGER, 0);
    }

    /**
     * 아이템이 DreamWork 아이템인지 확인
     */
    public boolean isDreamItem(ItemStack item) {
        return getDreamItemId(item) != null;
    }

    /**
     * 아이템이 특정 ID인지 확인 (엄격한 검증)
     */
    public boolean isItem(ItemStack item, String id) {
        String itemId = getDreamItemId(item);
        if (itemId == null || !itemId.equals(id))
            return false;

        // 추가 검증: 템플릿과 재질/모델데이터 일치 여부 확인
        DreamItemTemplate template = itemCache.get(id);
        if (template != null) {
            if (item.getType() != template.getMaterial()) {
                return false;
            }
            if (template.getCustomModelData() > 0) {
                if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData() ||
                        item.getItemMeta().getCustomModelData() != template.getCustomModelData()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 템플릿 존재 여부 확인
     */
    public boolean hasTemplate(String id) {
        return itemCache.containsKey(id);
    }

    /**
     * 모든 아이템 ID 목록
     */
    public Set<String> getAllItemIds() {
        return new HashSet<>(itemCache.keySet());
    }

    /**
     * 색상 코드 변환
     */
    private String translateColors(String text) {
        if (text == null)
            return "";
        return text.replace("&", "§");
    }

    // ==================== NamespacedKey Getter ====================

    public NamespacedKey getKeyId() {
        return keyId;
    }

    public NamespacedKey getKeyCategory() {
        return keyCategory;
    }

    public NamespacedKey getKeyQuality() {
        return keyQuality;
    }

    public NamespacedKey getKeyTimestamp() {
        return keyTimestamp;
    }
}
