package com.dreamwork.shop.injector;

import org.bukkit.Material;

/**
 * 개별 상점 아이템 설정 데이터 클래스
 * 
 * shops/*.yml 파일의 items 섹션에서 로드된 개별 아이템 정보를 담습니다.
 * 
 * @author DreamWork Team
 */
public class DreamShopItemConfig {

    private String id; // 아이템 고유 ID
    private Material material; // 바닐라 아이템 Material
    private String dreamworkItemId; // DreamWork 커스텀 아이템 ID (null이면 바닐라)
    private String name; // 표시 이름 (null이면 기본값)
    private java.util.List<String> lore; // 설명 (null이면 기본값)
    private int slot; // GUI 슬롯 위치
    private double buyPrice; // 구매 가격 (-1이면 구매 불가)
    private double sellPrice; // 판매 가격 (-1이면 판매 불가)
    private boolean dynamicPricing; // 동적 가격 사용 여부
    private int maxStock; // 최대 재고 (동적 가격 계산용)

    // 기본 생성자
    public DreamShopItemConfig() {
    }

    // Getter & Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getDreamworkItemId() {
        return dreamworkItemId;
    }

    public void setDreamworkItemId(String dreamworkItemId) {
        this.dreamworkItemId = dreamworkItemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public java.util.List<String> getLore() {
        return lore;
    }

    public void setLore(java.util.List<String> lore) {
        this.lore = lore;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public boolean isDynamicPricing() {
        return dynamicPricing;
    }

    public void setDynamicPricing(boolean dynamicPricing) {
        this.dynamicPricing = dynamicPricing;
    }

    public int getMaxStock() {
        return maxStock;
    }

    public void setMaxStock(int maxStock) {
        this.maxStock = maxStock;
    }

    /**
     * 커스텀 아이템인지 확인
     */
    public boolean isCustomItem() {
        return dreamworkItemId != null && !dreamworkItemId.isEmpty();
    }

    /**
     * 구매 가능 여부
     */
    public boolean isBuyable() {
        return buyPrice > 0;
    }

    /**
     * 판매 가능 여부
     */
    public boolean isSellable() {
        return sellPrice > 0;
    }
}
