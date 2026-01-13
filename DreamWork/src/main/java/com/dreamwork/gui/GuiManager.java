package com.dreamwork.gui;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.gui.impl.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * GUI 관리자
 * 
 * 모든 GUI 생성 및 열기를 관리합니다.
 * YAML 기반의 동적 GUI 시스템을 지원합니다.
 * 
 * @author DreamWork Team
 */
public class GuiManager {

    private final DreamWorkPlugin plugin;
    private final Map<String, GuiTemplate> guiTemplates = new HashMap<>();

    public GuiManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 모든 GUI 설정 로드
     */
    public void loadGuis() {
        guiTemplates.clear();
        Map<String, FileConfiguration> configs = plugin.getConfigManager().getAllGuiConfigs();

        for (Map.Entry<String, FileConfiguration> entry : configs.entrySet()) {
            String id = entry.getKey();
            FileConfiguration config = entry.getValue();

            // 파일 내부의 루트 섹션들을 스캔
            for (String key : config.getKeys(false)) {
                if (config.isConfigurationSection(key)) {
                    loadGuiTemplate(key, config.getConfigurationSection(key));
                }
            }
        }

        plugin.log(Level.INFO, "GUI 템플릿 " + guiTemplates.size() + "개 로드 완료");
    }

    private void loadGuiTemplate(String id, ConfigurationSection section) {
        try {
            GuiTemplate template = new GuiTemplate();
            template.setId(id);
            template.setTitle(plugin.getConfigManager().translateColors(section.getString("title", "DreamWork GUI")));
            template.setRows(section.getInt("size", 27) / 9);

            String fillMat = section.getString("fill_item", "AIR");
            template.setFillItem(Material.getMaterial(fillMat));

            Map<Integer, GuiButton> buttons = new HashMap<>();
            ConfigurationSection itemsSec = section.getConfigurationSection("items");

            if (itemsSec != null) {
                for (String btnKey : itemsSec.getKeys(false)) {
                    ConfigurationSection btnSec = itemsSec.getConfigurationSection(btnKey);
                    if (btnSec == null)
                        continue;

                    GuiButton button = new GuiButton();
                    button.setName(plugin.getConfigManager().translateColors(btnSec.getString("name", "지정되지 않음")));

                    List<String> lore = btnSec.getStringList("lore");
                    // 번역
                    List<String> translatedLore = new ArrayList<>();
                    for (String l : lore) {
                        translatedLore.add(plugin.getConfigManager().translateColors(l));
                    }
                    button.setLore(translatedLore);

                    String matName = btnSec.getString("material", "STONE");
                    button.setMaterial(Material.getMaterial(matName));
                    button.setCustomModelData(btnSec.getInt("model_data", 0));
                    button.setCondition(btnSec.getString("condition"));

                    // 액션은 단일 문자열 또는 리스트로 올 수 있음
                    List<String> actions = new ArrayList<>();
                    if (btnSec.isList("action")) {
                        actions.addAll(btnSec.getStringList("action"));
                    } else if (btnSec.isString("action")) {
                        actions.add(btnSec.getString("action"));
                    }
                    button.setActions(actions);

                    // 슬롯 파싱 (단일 슬롯 또는 리스트)
                    List<Integer> slots = new ArrayList<>();
                    if (btnKey.matches("\\d+")) { // 키 자체가 슬롯 번호인 경우
                        slots.add(Integer.parseInt(btnKey));
                    } else if (btnSec.contains("slot")) {
                        if (btnSec.isList("slot")) {
                            slots.addAll(btnSec.getIntegerList("slot"));
                        } else {
                            slots.add(btnSec.getInt("slot"));
                        }
                    } else if (btnSec.contains("slots")) { // slots 필드 지원
                        slots.addAll(btnSec.getIntegerList("slots"));
                    }

                    button.setSlots(slots);

                    // 슬롯별로 맵에 저장 (참조용)
                    for (int s : slots) {
                        buttons.put(s, button);
                    }
                }
            }
            template.setButtons(buttons);
            guiTemplates.put(id, template);

        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 템플릿 로드 실패: " + id + " - " + e.getMessage());
        }
    }

    /**
     * 동적 GUI 열기
     */
    public void openGui(Player player, String id) {
        GuiTemplate template = guiTemplates.get(id);
        if (template != null) {
            DynamicGui gui = new DynamicGui(plugin, player, template);
            gui.open();
        } else {
            // 하드코딩된 GUI 메소드로 폴백 (호환성 유지)
            switch (id) {
                case "main_dashboard" -> openMainDashboard(player);
                case "job_detail" -> openJobDetailGui(player, com.dreamwork.job.JobType.MINER); // 기본값
                case "forge" -> openForgeGui(player);
                case "kitchen" -> openKitchenGui(player);
                case "fish_market" -> openFishMarketGui(player);
                case "mission_board" -> openMissionBoardGui(player);
                case "map_store" -> openMapStoreGui(player);
                default -> player.sendMessage("§c존재하지 않는 메뉴입니다: " + id);
            }
        }
    }

    /**
     * 메인 대시보드 열기
     */
    public void openMainDashboard(Player player) {
        try {
            MainDashboardGui gui = new MainDashboardGui(plugin, player);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: MainDashboard - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 직업 상세 정보 GUI 열기
     */
    public void openJobDetailGui(Player player, com.dreamwork.job.JobType jobType) {
        try {
            JobDetailGui gui = new JobDetailGui(plugin, player, jobType);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: JobDetail - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 대장간 GUI 열기
     */
    public void openForgeGui(Player player) {
        try {
            ForgeGui gui = new ForgeGui(plugin, player);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: Forge - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 주방 GUI 열기
     */
    public void openKitchenGui(Player player) {
        try {
            KitchenGui gui = new KitchenGui(plugin, player);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: Kitchen - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 어시장 GUI 열기
     */
    public void openFishMarketGui(Player player) {
        try {
            FishMarketGui gui = new FishMarketGui(plugin, player);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: FishMarket - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 미션 게시판 GUI 열기
     */
    public void openMissionBoardGui(Player player) {
        try {
            MissionBoardGui gui = new MissionBoardGui(plugin, player);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: MissionBoard - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 지도 상점 GUI 열기
     */
    public void openMapStoreGui(Player player) {
        try {
            MapStoreGui gui = new MapStoreGui(plugin, player);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: MapStore - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 대화 GUI 열기
     */
    public void openDialogueGui(Player player, com.dreamwork.dialogue.DialogueSession session) {
        try {
            DialogueGui gui = new DialogueGui(plugin, player, session);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: Dialogue - " + e.getMessage());
            player.sendMessage("§c대화를 시작할 수 없습니다.");
        }
    }

    /**
     * GUI 템플릿 가져오기
     */
    public GuiTemplate getTemplate(String id) {
        return guiTemplates.get(id);
    }

    /**
     * 설정 리로드
     */
    public void reload() {
        loadGuis();
        plugin.debug("GUI 매니저 리로드 완료");
    }
}
