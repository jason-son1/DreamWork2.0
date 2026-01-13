package com.dreamwork.dialogue;

/**
 * 대화 선택지
 * 
 * 플레이어가 선택할 수 있는 대화 옵션입니다.
 * 
 * @author DreamWork Team
 */
public class DialogueOption {

    private final String text; // 표시될 텍스트
    private final String nextNodeId; // 다음 대화 노드 ID (null이면 액션만 실행)
    private final String action; // 실행할 액션 (예: "OPEN_GUI:forge")
    private final String condition; // 조건 (예: "!quest:daily.completed")

    public DialogueOption(String text, String nextNodeId, String action, String condition) {
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.action = action;
        this.condition = condition;
    }

    public String getText() {
        return text;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public String getAction() {
        return action;
    }

    public String getCondition() {
        return condition;
    }

    public boolean hasAction() {
        return action != null && !action.isEmpty();
    }

    public boolean hasCondition() {
        return condition != null && !condition.isEmpty();
    }

    public boolean hasNextNode() {
        return nextNodeId != null && !nextNodeId.isEmpty();
    }
}
