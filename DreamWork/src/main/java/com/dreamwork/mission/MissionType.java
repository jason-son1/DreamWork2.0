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
    /**
     * 블록 파괴 (광부, 농부)
     */
    BREAK,

    /**
     * 조건부 블록 파괴 (광부 기술 미션 등)
     */
    CONDITIONAL_BREAK,

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
     * 통계 누적 확인 (플레이 타임 등)
     */
    STATISTIC_ACCUMULATE,

    /**
     * NPC에게 납품
     */
    SUBMIT,

    /**
     * 아이템 기부 (NPC/블록)
     */
    ITEM_DONATION,

    /**
     * 작물 수확 (농부)
     */
    HARVEST,

    /**
     * 타운 세금 납부
     */
    TOWNY_TAX,

    /**
     * 상점 구매 (경제)
     */
    SHOP_PURCHASE,

    /**
     * 타운 홈 방문 (사회)
     */
    VISIT_TOWN_HOME,

    /**
     * 타운 내 블록 설치 (사회)
     */
    PLACE_BLOCK_IN_TOWN,

    /**
     * 특정 채널 채팅 (사회)
     */
    CHAT_CHANNEL,

    /**
     * 블록 상호작용 (표지판 찾기 등)
     */
    INTERACT_BLOCK,

    /**
     * 조건부 엔티티 처치 (헤드샷, 크리티컬 등)
     */
    CONDITIONAL_KILL,

    /**
     * 조건부 낚시 (미끼, 날씨 등)
     */
    CONDITIONAL_FISH,

    /**
     * 배달 (시간 제한, 텔레포트 금지)
     */
    DELIVERY,

    /**
     * 바이옴 방문
     */
    VISIT_BIOME,

    /**
     * 구조물 발견
     */
    DISCOVER_STRUCTURE,

    /**
     * 아이템 수집 (인벤토리 체크)
     */
    COLLECT,

    /**
     * 커스텀 체크 (지도 완성 등)
     */
    CUSTOM_CHECK,

    /**
     * 배달 미션 (가독성 위해 추가)
     */
    DELIVERY_MISSION,

    /**
     * NPC 상호작용
     */
    INTERACT_NPC
}
