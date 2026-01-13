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

    private String chainId; // 미션 체인 ID
    private List<String> description; // 미션 설명 (Lore)
    private org.bukkit.Material icon; // GUI 아이콘
    private Map<String, Object> parsedConditions; // 파싱된 조건 목록

    // Complex Rewards
    private List<String> rewardCommands;
    private List<String> rewardPermissions;
    private String rewardTitle;
    private String rewardSubtitle;
    private List<String> rewardBuffs; // Format: EFFECT:LEVEL:DURATION

    public MissionTemplate() {
        this.targets = new ArrayList<>();
        this.conditions = new ArrayList<>();
        this.rewardJobExp = new HashMap<>();
        this.rewardItems = new ArrayList<>();
        this.description = new ArrayList<>();
        this.parsedConditions = new HashMap<>();
        this.rewardCommands = new ArrayList<>();
        this.rewardPermissions = new ArrayList<>();
        this.rewardBuffs = new ArrayList<>();
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

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public org.bukkit.Material getIcon() {
        return icon;
    }

    public void setIcon(org.bukkit.Material icon) {
        this.icon = icon;
    }

    public Map<String, Object> getParsedConditions() {
        return parsedConditions;
    }

    public void setParsedConditions(Map<String, Object> parsedConditions) {
        this.parsedConditions = parsedConditions;
    }

    public List<String> getRewardCommands() {
        return rewardCommands;
    }

    public void setRewardCommands(List<String> rewardCommands) {
        this.rewardCommands = rewardCommands;
    }

    public List<String> getRewardPermissions() {
        return rewardPermissions;
    }

    public void setRewardPermissions(List<String> rewardPermissions) {
        this.rewardPermissions = rewardPermissions;
    }

    public String getRewardTitle() {
        return rewardTitle;
    }

    public void setRewardTitle(String rewardTitle) {
        this.rewardTitle = rewardTitle;
    }

    public String getRewardSubtitle() {
        return rewardSubtitle;
    }

    public void setRewardSubtitle(String rewardSubtitle) {
        this.rewardSubtitle = rewardSubtitle;
    }

    public List<String> getRewardBuffs() {
        return rewardBuffs;
    }

    public void setRewardBuffs(List<String> rewardBuffs) {
        this.rewardBuffs = rewardBuffs;
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
