package com.dreamwork.mission;

/**
 * 미션 타입 열거형
 * 
 * @author DreamWork Team
 */
public enum MissionType {
    /**
     * 블록 파괴 (광부, 농부)
     */
    BREAK,

    /**
     * 엔티티 처치 (사냥꾼)
     */
    KILL,

    /**
     * 낚시 (어부)
     */
    FISH,

    /**
     * 아이템 제작
     */
    CRAFT,

    /**
     * 아이템 섭취
     */
    EAT,

    /**
     * 이동/탐험 (탐험가)
     */
    WALK,

    /**
     * NPC에게 납품
     */
    SUBMIT
}
