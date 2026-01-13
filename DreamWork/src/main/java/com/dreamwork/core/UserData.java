package com.dreamwork.core;

import com.dreamwork.job.JobType;
import com.dreamwork.mission.MissionStatus;
import com.dreamwork.mission.PlayerMissionData;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 유저 데이터 클래스
 * 
 * 메모리에서 플레이어의 직업 데이터를 관리합니다.
 * 
 * @author DreamWork Team
 */
public class UserData {

    private final UUID uuid;

    // 직업별 레벨
    private final Map<JobType, Integer> jobLevels = new EnumMap<>(JobType.class);

    // 직업별 현재 경험치
    private final Map<JobType, Double> jobExps = new EnumMap<>(JobType.class);

    // 직업별 총 누적 경험치 (통계용)
    private final Map<JobType, Double> totalExps = new EnumMap<>(JobType.class);

    // 마지막 액션 시간 (쿨다운 체크용)
    private final Map<String, Long> lastActionTimes = new java.util.HashMap<>();

    // 미션 초기화 시간
    private long lastDailyReset = 0;
    private long lastWeeklyReset = 0;

    // 미션 데이터 (미션 ID -> 데이터)
    private final Map<String, PlayerMissionData> missions = new HashMap<>();

    public UserData(UUID uuid) {
        this.uuid = uuid;

        // 모든 직업 초기화 (레벨 1, 경험치 0)
        for (JobType jobType : JobType.values()) {
            jobLevels.put(jobType, 1);
            jobExps.put(jobType, 0.0);
            totalExps.put(jobType, 0.0);
        }
    }

    // ==================== UUID ====================

    public UUID getUuid() {
        return uuid;
    }

    // ==================== 레벨 관련 ====================

    /**
     * 직업 레벨 가져오기
     */
    public int getJobLevel(JobType jobType) {
        return jobLevels.getOrDefault(jobType, 1);
    }

    /**
     * 직업 레벨 설정
     */
    public void setJobLevel(JobType jobType, int level) {
        jobLevels.put(jobType, Math.max(1, level));
    }

    /**
     * 직업 레벨 증가
     */
    public void addJobLevel(JobType jobType, int amount) {
        setJobLevel(jobType, getJobLevel(jobType) + amount);
    }

    // ==================== 경험치 관련 ====================

    /**
     * 현재 경험치 가져오기
     */
    public double getJobExp(JobType jobType) {
        return jobExps.getOrDefault(jobType, 0.0);
    }

    /**
     * 현재 경험치 설정
     */
    public void setJobExp(JobType jobType, double exp) {
        jobExps.put(jobType, Math.max(0, exp));
    }

    /**
     * 경험치 추가
     */
    public void addJobExp(JobType jobType, double amount) {
        setJobExp(jobType, getJobExp(jobType) + amount);
        addTotalExp(jobType, amount);
    }

    // ==================== 총 경험치 (통계) ====================

    /**
     * 총 누적 경험치 가져오기
     */
    public double getTotalExp(JobType jobType) {
        return totalExps.getOrDefault(jobType, 0.0);
    }

    /**
     * 총 누적 경험치 설정
     */
    public void setTotalExp(JobType jobType, double exp) {
        totalExps.put(jobType, Math.max(0, exp));
    }

    /**
     * 총 누적 경험치 추가
     */
    public void addTotalExp(JobType jobType, double amount) {
        setTotalExp(jobType, getTotalExp(jobType) + amount);
    }

    // ==================== 쿨다운 관련 ====================

    /**
     * 마지막 액션 시간 가져오기
     */
    public long getLastActionTime(String actionKey) {
        return lastActionTimes.getOrDefault(actionKey, 0L);
    }

    /**
     * 마지막 액션 시간 설정
     */
    public void setLastActionTime(String actionKey, long time) {
        lastActionTimes.put(actionKey, time);
    }

    /**
     * 쿨다운 체크 (밀리초 단위)
     * 
     * @return true면 쿨다운 중, false면 사용 가능
     */
    public boolean isOnCooldown(String actionKey, long cooldownMs) {
        long lastTime = getLastActionTime(actionKey);
        return System.currentTimeMillis() - lastTime < cooldownMs;
    }

    /**
     * 쿨다운 업데이트 및 체크
     * 
     * @return true면 사용 가능 (쿨다운 아님), false면 쿨다운 중
     */
    public boolean checkAndUpdateCooldown(String actionKey, long cooldownMs) {
        if (isOnCooldown(actionKey, cooldownMs)) {
            return false;
        }
        setLastActionTime(actionKey, System.currentTimeMillis());
        return true;
    }

    // ==================== 미션 관련 ====================

    /**
     * 미션 데이터 가져오기 (없으면 생성하지 않음)
     */
    public PlayerMissionData getMission(String missionId) {
        return missions.get(missionId);
    }

    /**
     * 미션 데이터 가져오기 (없으면 생성)
     */
    public PlayerMissionData getOrCreateMission(String missionId) {
        return missions.computeIfAbsent(missionId, PlayerMissionData::new);
    }

    /**
     * 미션 상태 확인
     */
    public MissionStatus getMissionStatus(String missionId) {
        PlayerMissionData data = missions.get(missionId);
        return data != null ? data.getStatus() : MissionStatus.NOT_STARTED;
    }

    /**
     * 미션 진행도 업데이트
     */
    public void updateMissionProgress(String missionId, int amount, int maxAmount) {
        PlayerMissionData data = getOrCreateMission(missionId);

        // 이미 완료된 미션은 무시 (보상 수령 전이라도 진행도는 안 오름)
        if (data.isCompleted())
            return;

        // 상태가 IN_PROGRESS가 아니면 시작 처리
        if (data.getStatus() == MissionStatus.NOT_STARTED) {
            data.setStatus(MissionStatus.IN_PROGRESS);
        }

        data.addProgress(amount);

        // 목표 달성 체크
        if (data.getProgress() >= maxAmount) {
            data.setProgress(maxAmount);
            data.setStatus(MissionStatus.COMPLETED);
        }
    }

    /**
     * 미션 초기화 (일일/주간 미션 갱신용)
     */
    public void resetMission(String missionId) {
        missions.remove(missionId);
    }

    /**
     * 진행 중인 모든 미션 반환
     */
    public Map<String, PlayerMissionData> getAllMissions() {
        return new HashMap<>(missions);
    }

    // ==================== 유틸리티 ====================

    /**
     * 모든 직업의 평균 레벨
     */
    public double getAverageLevel() {
        double total = 0;
        for (JobType jobType : JobType.values()) {
            total += getJobLevel(jobType);
        }
        return total / JobType.values().length;
    }

    /**
     * 가장 높은 레벨의 직업
     */
    public JobType getHighestLevelJob() {
        JobType highest = JobType.MINER;
        int highestLevel = 0;

        for (JobType jobType : JobType.values()) {
            int level = getJobLevel(jobType);
            if (level > highestLevel) {
                highestLevel = level;
                highest = jobType;
            }
        }

        return highest;
    }

    /**
     * 총 레벨 합계
     */
    public int getTotalLevels() {
        int total = 0;
        for (JobType jobType : JobType.values()) {
            total += getJobLevel(jobType);
        }
        return total;
    }

    public long getLastDailyReset() {
        return lastDailyReset;
    }

    public void setLastDailyReset(long lastDailyReset) {
        this.lastDailyReset = lastDailyReset;
    }

    public long getLastWeeklyReset() {
        return lastWeeklyReset;
    }

    public void setLastWeeklyReset(long lastWeeklyReset) {
        this.lastWeeklyReset = lastWeeklyReset;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UserData[uuid=").append(uuid).append(", jobs={");

        boolean first = true;
        for (JobType jobType : JobType.values()) {
            if (!first)
                sb.append(", ");
            sb.append(jobType.name()).append("=Lv.")
                    .append(getJobLevel(jobType));
            first = false;
        }

        sb.append("}]");
        return sb.toString();
    }
}
