package com.dreamwork.dialogue;

import java.util.Map;

/**
 * 대화 트리
 * 
 * NPC 한 명의 전체 대화 구조입니다.
 * 
 * @author DreamWork Team
 */
public class DialogueTree {

    private final String npcId; // NPC 식별자
    private final String startNodeId; // 시작 노드 ID
    private final Map<String, DialogueNode> nodes; // 모든 노드

    public DialogueTree(String npcId, String startNodeId, Map<String, DialogueNode> nodes) {
        this.npcId = npcId;
        this.startNodeId = startNodeId;
        this.nodes = nodes;
    }

    public String getNpcId() {
        return npcId;
    }

    public String getStartNodeId() {
        return startNodeId;
    }

    public DialogueNode getStartNode() {
        return nodes.get(startNodeId);
    }

    public DialogueNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public Map<String, DialogueNode> getAllNodes() {
        return nodes;
    }
}
