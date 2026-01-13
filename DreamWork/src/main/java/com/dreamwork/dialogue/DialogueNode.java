package com.dreamwork.dialogue;

import java.util.List;

/**
 * 대화 노드
 * 
 * NPC의 대사 한 단락과 선택지 목록을 담습니다.
 * 
 * @author DreamWork Team
 */
public class DialogueNode {

    private final String id; // 노드 ID
    private final String speaker; // 화자 이름 (NPC 이름)
    private final String text; // 대사 내용
    private final List<DialogueOption> options; // 선택지 목록

    public DialogueNode(String id, String speaker, String text, List<DialogueOption> options) {
        this.id = id;
        this.speaker = speaker;
        this.text = text;
        this.options = options;
    }

    public String getId() {
        return id;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getText() {
        return text;
    }

    public List<DialogueOption> getOptions() {
        return options;
    }

    public boolean hasOptions() {
        return options != null && !options.isEmpty();
    }

    /**
     * 옵션이 없으면 대화 종료 노드임
     */
    public boolean isEndNode() {
        return !hasOptions();
    }
}
