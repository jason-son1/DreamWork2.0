package com.dreamwork.npc;

import com.dreamwork.DreamWorkPlugin;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * NPC 상호작용 핸들러
 * 
 * Citizens NPC 클릭 시 대화를 시작하거나 GUI를 엽니다.
 * 대화 스크립트가 있으면 대화를 우선 시작합니다.
 * 
 * @author DreamWork Team
 */
public class NpcHandler implements Listener {

    private final DreamWorkPlugin plugin;

    public NpcHandler(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        String npcName = event.getNPC().getName();
        Player player = event.getClicker();
        FileConfiguration config = plugin.getConfigManager().getConfig();

        // NPC ID 결정 (config에서 매핑된 이름과 비교)
        String npcId = getNpcId(npcName, config);

        // 1. NPC 상호작용 미션 트리거
        plugin.getMissionManager().processEvent(player, com.dreamwork.mission.MissionType.INTERACT_NPC, npcName, 1);
        if (npcId != null) {
            plugin.getMissionManager().processEvent(player, com.dreamwork.mission.MissionType.DELIVERY, npcId, 1);
            plugin.getMissionManager().processEvent(player, com.dreamwork.mission.MissionType.INTERACT_NPC, npcId, 1);
        }

        if (npcId == null) {
            // 매핑되지 않은 NPC
            return;
        }

        // 대화 스크립트가 있는지 확인
        if (plugin.getDialogueManager() != null && plugin.getDialogueManager().hasDialogue(npcId)) {
            // 대화 시작
            plugin.getDialogueManager().startDialogue(player, npcId);
        } else {
            // 대화 스크립트가 없으면 직접 GUI 열기
            openGuiForNpc(player, npcId);
        }
    }

    /**
     * NPC 이름으로 ID 결정
     */
    private String getNpcId(String npcName, FileConfiguration config) {
        if (npcName.contains(config.getString("npcs.blacksmith", "대장장이"))) {
            return "blacksmith";
        } else if (npcName.contains(config.getString("npcs.chef", "요리사"))) {
            return "chef";
        } else if (npcName.contains(config.getString("npcs.fisher", "낚시꾼"))) {
            return "fisher";
        } else if (npcName.contains(config.getString("npcs.explorer", "탐험가"))) {
            return "explorer";
        } else if (npcName.contains(config.getString("npcs.job_master", "직업 마스터"))) {
            return "job_master";
        }
        return null;
    }

    /**
     * NPC ID에 따라 GUI 열기 (대화 스크립트가 없을 때)
     */
    private void openGuiForNpc(Player player, String npcId) {
        switch (npcId) {
            case "blacksmith" -> plugin.getGuiManager().openForgeGui(player);
            case "chef" -> plugin.getGuiManager().openKitchenGui(player);
            case "fisher" -> plugin.getGuiManager().openFishMarketGui(player);
            case "explorer" -> plugin.getGuiManager().openMapStoreGui(player);
            case "job_master" -> plugin.getGuiManager().openMainDashboard(player);
        }
    }
}
