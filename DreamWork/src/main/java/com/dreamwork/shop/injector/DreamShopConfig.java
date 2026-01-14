package com.dreamwork.shop.injector;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;

/**
 * 상점 섹션 설정 데이터 클래스
 * 
 * shops/*.yml 파일에서 로드된 전체 상점 설정을 담습니다.
 * 
 * @author DreamWork Team
 */
public class DreamShopConfig {

    private String sectionId; // 상점 고유 ID (예: "dw_miner")
    private String displayName; // 표시 이름 (색상 코드 포함)
    private String description; // 설명
    private Material icon; // 아이콘 Material
    private String permission; // 접근 권한 (빈 문자열이면 제한 없음)
    private String economy; // 사용 화폐 시스템 (기본: "Vault")
    private int rows; // GUI 행 수 (1~6)
    private List<DreamShopItemConfig> items; // 아이템 목록

    // 경험치 보너스 설정
    private boolean jobExpEnabled; // 직업 경험치 지급 여부
    private double baseExpPerSale; // 판매 건당 기본 경험치
    private double priceExpRatio; // 판매 금액 대비 경험치 비율

    // 기본 생성자
    public DreamShopConfig() {
        this.items = new ArrayList<>();
        this.economy = "Vault";
        this.rows = 6;
        this.jobExpEnabled = true;
        this.baseExpPerSale = 1.0;
        this.priceExpRatio = 0.01;
    }

    // Getter & Setter
    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Material getIcon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getEconomy() {
        return economy;
    }

    public void setEconomy(String economy) {
        this.economy = economy;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = Math.max(1, Math.min(6, rows)); // 1~6 제한
    }

    public List<DreamShopItemConfig> getItems() {
        return items;
    }

    public void setItems(List<DreamShopItemConfig> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void addItem(DreamShopItemConfig item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    public boolean isJobExpEnabled() {
        return jobExpEnabled;
    }

    public void setJobExpEnabled(boolean jobExpEnabled) {
        this.jobExpEnabled = jobExpEnabled;
    }

    public double getBaseExpPerSale() {
        return baseExpPerSale;
    }

    public void setBaseExpPerSale(double baseExpPerSale) {
        this.baseExpPerSale = baseExpPerSale;
    }

    public double getPriceExpRatio() {
        return priceExpRatio;
    }

    public void setPriceExpRatio(double priceExpRatio) {
        this.priceExpRatio = priceExpRatio;
    }

    /**
     * 직업 타입 추출 (sectionId에서 "dw_" 제거)
     */
    public String getJobType() {
        if (sectionId != null && sectionId.startsWith("dw_")) {
            return sectionId.substring(3).toUpperCase();
        }
        return null;
    }

    /**
     * 슬롯 수 계산 (rows * 9)
     */
    public int getSlotCount() {
        return rows * 9;
    }
}
