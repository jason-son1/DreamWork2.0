package com.dreamwork.shop;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.gui.GuiTemplate;
import com.dreamwork.gui.impl.DynamicGui;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * 상점 GUI
 * 
 * DynamicGui를 상속받아 상점 기능을 추가합니다.
 * 
 * @author DreamWork Team
 */
public class ShopGui extends DynamicGui {

    private final ShopManager.ShopData shopData;

    public ShopGui(DreamWorkPlugin plugin, Player player, String shopId) {
        super(plugin, player, plugin.getGuiManager().getTemplate(shopId) != null
                ? plugin.getGuiManager().getTemplate(shopId)
                : new GuiTemplate()); // 템플릿 없으면 빈 템플릿 (오류 방지)

        this.shopData = plugin.getShopManager().getShop(shopId);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        super.onClick(event);

        // TODO: 상점 로직 구현 (아이템 구매/판매)
    }
}
