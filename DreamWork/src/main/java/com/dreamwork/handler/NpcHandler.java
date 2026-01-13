package com.dreamwork.handler;

import com.dreamwork.DreamWorkPlugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * NPC 상호작용 핸들러
 * 
 * Citizens NPC 또는 일반 엔티티와 상호작용했을 때
 * 해당 NPC의 이름이나 역할에 따라 적절한 GUI를 열어줍니다.
 * 
 * @author DreamWork Team
 */
public class NpcHandler implements Listener {

    private final DreamWorkPlugin plugin;

    public NpcHandler(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Citizens NPC 확인 (API가 있다면)
        // 여기서는 간단하게 이름 기반으로 체크 (Citizens가 없어도 이름표로 작동하도록)
        String npcName = ChatColor
                .stripColor(entity.getCustomName() != null ? entity.getCustomName() : entity.getName());

        if (npcName == null)
            return;

        // 이름 또는 특징에 따라 GUI 연결
        if (containsKeyword(npcName, "대장장이", "Blacksmith", "제련")) {
            event.setCancelled(true);
            plugin.getGuiManager().openForgeGui(player);
        } else if (containsKeyword(npcName, "요리사", "Chef", "주방")) {
            event.setCancelled(true);
            plugin.getGuiManager().openKitchenGui(player);
        } else if (containsKeyword(npcName, "생선", "Fish", "상인", "Merchant")) {
            event.setCancelled(true);
            plugin.getGuiManager().openFishMarketGui(player);
        } else if (containsKeyword(npcName, "게시판", "Board", "의뢰", "Mission")) {
            event.setCancelled(true);
            plugin.getGuiManager().openMissionBoardGui(player);
        } else if (containsKeyword(npcName, "지도", "Map", "탐험")) {
            event.setCancelled(true);
            plugin.getGuiManager().openMapStoreGui(player);
        }
        // 직업 전직소 등 추가 가능
    }

    private boolean containsKeyword(String target, String... keywords) {
        for (String keyword : keywords) {
            if (target.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
