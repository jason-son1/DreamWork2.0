package com.dreamwork.mission;

/**
 * 미션 상태 열거형
 */
public enum MissionStatus {
    /** 미수락 상태 */
    NOT_STARTED,

    /** 진행 중 */
    IN_PROGRESS,

    /** 목표 달성 (보상 미수령) */
    COMPLETED,

    /** 보상 수령 완료 */
    CLAIMED
}
