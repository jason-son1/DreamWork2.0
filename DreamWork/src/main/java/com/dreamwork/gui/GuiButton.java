package com.dreamwork.gui;

import org.bukkit.Material;
import java.util.List;

/**
 * GUI 버튼
 * 
 * GUI 템플릿 내의 개별 버튼 정보를 정의합니다.
 * 
 * @author DreamWork Team
 */
public class GuiButton {

    private List<Integer> slots;
    private Material material;
    private String name;
    private List<String> lore;
    private int customModelData;
    private List<String> actions;
    private String condition;

    public List<Integer> getSlots() {
        return slots;
    }

    public void setSlots(List<Integer> slots) {
        this.slots = slots;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
