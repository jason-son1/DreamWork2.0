package com.dreamwork.item;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 아이템 템플릿 클래스
 * 
 * YAML 설정에서 로드된 아이템 정보를 저장하는 데이터 클래스입니다.
 * 
 * @author DreamWork Team
 */
public class DreamItemTemplate {

    // 기본 정보
    private String id; // 고유 ID
    private String category; // 카테고리 (minerals, crops, fishes 등)
    private Material material; // 바닐라 재질
    private String displayName; // 표시 이름
    private List<String> lore; // 설명

    // 메타데이터
    private int quality; // 품질/등급 (1~3성 등)
    private int customModelData; // 커스텀 모델 데이터 (리소스팩)
    private boolean glowing; // 인챈트 글로우 효과

    // PDC 데이터
    private Map<String, Object> pdcData; // 추가 PDC 데이터

    public DreamItemTemplate() {
        this.lore = new ArrayList<>();
        this.pdcData = new HashMap<>();
    }

    // ==================== Getter & Setter ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getLore() {
        return lore != null ? lore : new ArrayList<>();
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public Map<String, Object> getPdcData() {
        return pdcData != null ? pdcData : new HashMap<>();
    }

    public void setPdcData(Map<String, Object> pdcData) {
        this.pdcData = pdcData;
    }

    // ==================== 유틸리티 ====================

    /**
     * PDC 데이터에서 특정 키의 값 가져오기
     */
    public Object getPdcValue(String key) {
        return pdcData != null ? pdcData.get(key) : null;
    }

    /**
     * PDC 데이터에 값 추가
     */
    public void setPdcValue(String key, Object value) {
        if (pdcData == null) {
            pdcData = new HashMap<>();
        }
        pdcData.put(key, value);
    }

    @Override
    public String toString() {
        return "DreamItemTemplate{" +
                "id='" + id + '\'' +
                ", category='" + category + '\'' +
                ", material=" + material +
                ", displayName='" + displayName + '\'' +
                ", quality=" + quality +
                '}';
    }
}
