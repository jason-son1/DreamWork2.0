package com.dreamwork.gui;

import org.bukkit.Material;
import java.util.List;
import java.util.Map;

/**
 * GUI 템플릿
 * 
 * YAML 설정 파일에서 로드된 GUI 레이아웃 정보를 저장합니다.
 * 
 * @author DreamWork Team
 */
public class GuiTemplate {

    private String id;
    private String title;
    private int rows;
    private Material fillItem;
    private Map<Integer, GuiButton> buttons;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public Material getFillItem() {
        return fillItem;
    }

    public void setFillItem(Material fillItem) {
        this.fillItem = fillItem;
    }

    public Map<Integer, GuiButton> getButtons() {
        return buttons;
    }

    public void setButtons(Map<Integer, GuiButton> buttons) {
        this.buttons = buttons;
    }
}
