package com.dreamwork.dialogue;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 대화 세션
 * 
 * 플레이어의 현재 대화 진행 상태를 저장합니다.
 * 
 * @author DreamWork Team
 */
public class DialogueSession {

    private final UUID playerUuid;
    private final DialogueTree tree;
    private DialogueNode currentNode;

    public DialogueSession(Player player, DialogueTree tree, DialogueNode startNode) {
        this.playerUuid = player.getUniqueId();
        this.tree = tree;
        this.currentNode = startNode;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public DialogueTree getTree() {
        return tree;
    }

    public DialogueNode getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(DialogueNode node) {
        this.currentNode = node;
    }

    public String getNpcId() {
        return tree.getNpcId();
    }
}
