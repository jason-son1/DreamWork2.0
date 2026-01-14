package com.dreamwork.shop.injector;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * DreamWork 아이템을 EconomyShop ShopItem으로 변환하는 변환기
 * 
 * 리플렉션을 사용하여 EconomyShop API 호환성을 유지합니다.
 * 
 * @author DreamWork Team
 */
public class ItemConverter {

    private final DreamWorkPlugin plugin;

    public ItemConverter(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * DreamShopItemConfig를 EconomyShop ShopItem으로 변환 (리플렉션 사용)
     */
    public Object convert(DreamShopItemConfig config) {
        try {
            // ItemStack 생성
            ItemStack itemStack = createItemStack(config);

            // ShopItem 클래스 로드
            Class<?> shopItemClass = Class.forName("me.antigravity.economyshop.model.ShopItem");

            // builder() 메서드 호출
            Method builderMethod = shopItemClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            Class<?> builderClass = builder.getClass();

            // 빌더 메서드 호출
            builder = builderClass.getMethod("id", String.class).invoke(builder, config.getId());
            builder = builderClass.getMethod("itemStack", ItemStack.class).invoke(builder, itemStack);
            builder = builderClass.getMethod("slot", int.class).invoke(builder, config.getSlot());

            // 가격 설정
            if (config.isBuyable()) {
                builder = builderClass.getMethod("buyPrice", double.class).invoke(builder, config.getBuyPrice());
            }
            if (config.isSellable()) {
                builder = builderClass.getMethod("sellPrice", double.class).invoke(builder, config.getSellPrice());
            }

            // 동적 가격 설정
            if (config.isDynamicPricing()) {
                try {
                    builder = builderClass.getMethod("dynamicPricing", boolean.class).invoke(builder, true);
                    builder = builderClass.getMethod("maxStock", int.class).invoke(builder, config.getMaxStock());
                    builder = builderClass.getMethod("currentStock", int.class).invoke(builder,
                            config.getMaxStock() / 2);
                } catch (NoSuchMethodException e) {
                    plugin.debug("동적 가격 메서드를 찾을 수 없음 (무시)");
                }
            }

            // build() 호출
            return builderClass.getMethod("build").invoke(builder);

        } catch (Exception e) {
            plugin.debug("ShopItem 변환 실패: " + config.getId() + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 설정에서 ItemStack 생성
     */
    public ItemStack createItemStack(DreamShopItemConfig config) {
        ItemStack item;

        // DreamWork 커스텀 아이템인 경우
        if (config.isCustomItem()) {
            ItemStack customItem = plugin.getItemManager().createItem(config.getDreamworkItemId(), 1);
            if (customItem != null) {
                item = customItem;
            } else {
                plugin.debug("커스텀 아이템을 찾을 수 없음: " + config.getDreamworkItemId());
                Material mat = config.getMaterial() != null ? config.getMaterial() : Material.BARRIER;
                item = new ItemStack(mat);
            }
        } else {
            Material mat = config.getMaterial() != null ? config.getMaterial() : Material.STONE;
            item = new ItemStack(mat);
        }

        // 이름과 설명 설정
        if (config.getName() != null || config.getLore() != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (config.getName() != null) {
                    meta.setDisplayName(translateColors(config.getName()));
                }
                if (config.getLore() != null && !config.getLore().isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : config.getLore()) {
                        coloredLore.add(translateColors(line));
                    }
                    meta.setLore(coloredLore);
                }
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private String translateColors(String text) {
        if (text == null)
            return null;
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
