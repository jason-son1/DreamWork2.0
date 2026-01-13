package com.dreamwork.hook;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.logging.Level;

/**
 * Citizens NPC 시스템 Hook
 * 
 * Citizens API를 통해 NPC와의 상호작용을 처리합니다.
 * NPC 우클릭 시 DreamWork의 GUI를 열거나 기능을 실행합니다.
 * 
 * @author DreamWork Team
 */
public class CitizensHook implements Listener {

    private final DreamWorkPlugin plugin;
    private boolean enabled = false;

    public CitizensHook(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    /**
     * Citizens 연동 설정
     */
    private void setup() {
        if (Bukkit.getPluginManager().getPlugin("Citizens") != null) {
            enabled = true;
            // 이벤트 리스너 등록
            Bukkit.getPluginManager().registerEvents(this, plugin);
            plugin.debug("Citizens 플러그인 감지됨");
        } else {
            plugin.debug("Citizens 플러그인을 찾을 수 없습니다 (선택적 기능)");
        }
    }

    /**
     * 연동 상태 확인
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * NPC 우클릭 이벤트 처리
     * Citizens의 NPCRightClickEvent를 통해 NPC 상호작용을 감지합니다.
     */
    @EventHandler
    public void onNPCRightClick(net.citizensnpcs.api.event.NPCRightClickEvent event) {
        if (!enabled)
            return;

        Player player = event.getClicker();
        net.citizensnpcs.api.npc.NPC npc = event.getNPC();
        int npcId = npc.getId();

        plugin.debug(player.getName() + "이(가) NPC #" + npcId + " (" + npc.getName() + ") 클릭");

        // 1. 대화 시스템 확인 (우선 순위)
        // NPC ID 또는 이름으로 대화 스크립트 검색
        String idStr = String.valueOf(npcId);
        if (plugin.getDialogueManager().hasDialogue(idStr)) {
            plugin.getDialogueManager().startDialogue(player, idStr);
            return;
        }

        // 2. NPC 역할 확인 (Hardcoded logic / npcs.yml)
        String npcRole = getNPCRole(npcId);

        if (npcRole != null) {
            handleNPCInteraction(player, npcRole, npcId);
        }
    }

    /**
     * NPC ID로 역할 조회
     * npcs.yml 설정에서 읽어옵니다.
     */
    private String getNPCRole(int npcId) {
        // TODO: npcs.yml에서 NPC 역할 매핑 구현
        // 임시로 하드코딩된 값 반환
        return null;
    }

    /**
     * NPC 상호작용 처리
     */
    private void handleNPCInteraction(Player player, String role, int npcId) {
        switch (role.toLowerCase()) {
            case "blacksmith":
                // 대장간 GUI 열기
                plugin.getGuiManager().openForgeGui(player);
                break;

            case "fishmonger":
                // 어시장 GUI 열기
                plugin.getGuiManager().openFishMarketGui(player);
                break;

            case "chef":
                // 주방 GUI 열기
                plugin.getGuiManager().openKitchenGui(player);
                break;

            case "quest_giver":
                // 미션 GUI 열기
                plugin.getGuiManager().openMissionBoardGui(player);
                break;

            default:
                plugin.debug("알 수 없는 NPC 역할: " + role);
                break;
        }
    }

    /**
     * NPC가 DreamWork NPC인지 확인
     */
    public boolean isDreamWorkNPC(int npcId) {
        return getNPCRole(npcId) != null;
    }
}
