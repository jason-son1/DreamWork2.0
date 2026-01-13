package com.dreamwork.mission;

/**
 * 플레이어 개별 미션 데이터
 * 
 * 플레이어가 진행 중이거나 완료한 미션의 상태를 저장합니다.
 * 
 * @author DreamWork Team
 */
public class PlayerMissionData {

    private final String missionId;
    private int progress;
    private MissionStatus status;
    private long startTime;
    private long completedTime;

    public PlayerMissionData(String missionId) {
        this.missionId = missionId;
        this.progress = 0;
        this.status = MissionStatus.NOT_STARTED;
        this.startTime = System.currentTimeMillis();
        this.completedTime = 0;
    }

    public String getMissionId() {
        return missionId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void addProgress(int amount) {
        this.progress += amount;
    }

    public MissionStatus getStatus() {
        return status;
    }

    public void setStatus(MissionStatus status) {
        this.status = status;
        if (status == MissionStatus.COMPLETED && completedTime == 0) {
            this.completedTime = System.currentTimeMillis();
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public long getCompletedTime() {
        return completedTime;
    }

    public boolean isCompleted() {
        return status == MissionStatus.COMPLETED || status == MissionStatus.CLAIMED;
    }
}
