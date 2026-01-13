package com.dreamwork.mission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 미션 템플릿 클래스
 * 
 * YAML에서 로드된 미션 정보를 저장합니다.
 * 
 * @author DreamWork Team
 */
public class MissionTemplate {

    private String id;
    private String displayName;
    private MissionType type;
    private List<String> targets;
    private int amount;
    private List<String> conditions;

    // 보상
    private double rewardMoney;
    private Map<String, Double> rewardJobExp;
    private List<String> rewardItems;

    // 연계 미션
    private String nextMission;

    // 초기화 주기 (DAILY, WEEKLY, ONE_TIME)
    private String resetCycle;

    public MissionTemplate() {
        this.targets = new ArrayList<>();
        this.conditions = new ArrayList<>();
        this.rewardJobExp = new HashMap<>();
        this.rewardItems = new ArrayList<>();
    }

    // ==================== Getter & Setter ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public MissionType getType() {
        return type;
    }

    public void setType(MissionType type) {
        this.type = type;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions;
    }

    public double getRewardMoney() {
        return rewardMoney;
    }

    public void setRewardMoney(double rewardMoney) {
        this.rewardMoney = rewardMoney;
    }

    public Map<String, Double> getRewardJobExp() {
        return rewardJobExp;
    }

    public void setRewardJobExp(Map<String, Double> rewardJobExp) {
        this.rewardJobExp = rewardJobExp;
    }

    public List<String> getRewardItems() {
        return rewardItems;
    }

    public void setRewardItems(List<String> rewardItems) {
        this.rewardItems = rewardItems;
    }

    public String getNextMission() {
        return nextMission;
    }

    public void setNextMission(String nextMission) {
        this.nextMission = nextMission;
    }

    public String getResetCycle() {
        return resetCycle;
    }

    public void setResetCycle(String resetCycle) {
        this.resetCycle = resetCycle;
    }

    /**
     * 타겟 검사
     */
    public boolean matchesTarget(String target) {
        if (targets == null || targets.isEmpty())
            return true;
        return targets.contains(target) || targets.contains(target.toUpperCase());
    }
}
