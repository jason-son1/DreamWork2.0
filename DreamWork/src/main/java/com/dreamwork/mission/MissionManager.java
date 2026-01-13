package com.dreamwork.mission;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.logging.Level;

/**
 * 미션 관리자
 * 
 * 미션 시스템의 핵심 로직을 담당합니다.
 * 미션 로드, 이벤트 처리, 진행도 관리, 보상 지급, 체인 시스템을 처리합니다.
 * 
 * @author DreamWork Team
 */
public class MissionManager {

    private final DreamWorkPlugin plugin;
    private final MissionLoader missionLoader;
    private final ConditionChecker conditionChecker;

    // 미션 템플릿 캐시 (ID -> Template)
    private final Map<String, MissionTemplate> missionCache = new HashMap<>();

    public MissionManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        this.missionLoader = new MissionLoader(plugin);
        this.conditionChecker = new ConditionChecker(plugin);
    }

    /**
     * 모든 미션 로드
     */
    public void loadMissions() {
        missionCache.clear();
        missionCache.putAll(missionLoader.loadAllMissions());
        plugin.log(Level.INFO, "미션 " + missionCache.size() + "개 로드 완료");
    }

    /**
     * 미션 수락 (강제 시작 등)
     */
    public void acceptMission(Player player, String missionId) {
        MissionTemplate template = getMission(missionId);
        if (template == null)
            return;

        com.dreamwork.core.UserData userData = plugin.getUserDataManager().getUserData(player);
        com.dreamwork.mission.PlayerMissionData data = userData.getOrCreateMission(missionId);

        // 이미 완료했거나 진행 중이면 무시
        if (data.getStatus() != com.dreamwork.mission.MissionStatus.NOT_STARTED) {
            return;
        }

        data.setStatus(com.dreamwork.mission.MissionStatus.IN_PROGRESS);
        player.sendMessage("§a[미션] §f" + template.getDisplayName() + " §a미션이 시작되었습니다!");
    }

    /**
     * 미션 이벤트 처리
     * 리스너에서 호출되어 미션 진행도를 업데이트합니다.
     */
    /**
     * 미션 이벤트 처리
     * 리스너에서 호출되어 미션 진행도를 업데이트합니다.
     */
    public void processEvent(Player player, MissionType type, String target, int amount) {
        processEvent(player, type, target, amount, null);
    }

    public void processEvent(Player player, MissionType type, String target, int amount, Map<String, Object> context) {
        com.dreamwork.core.UserData userData = plugin.getUserDataManager().getUserData(player);

        // 유저가 가진 모든 미션 중 '진행 중'인 것만 체크
        for (PlayerMissionData data : userData.getAllMissions().values()) {
            if (data.getStatus() != MissionStatus.IN_PROGRESS)
                continue;

            MissionTemplate template = getMission(data.getMissionId());
            if (template == null)
                continue;

            // 1. 미션 타입 확인
            if (template.getType() != type)
                continue;

            // 2. 타겟(대상) 확인
            if (!template.matchesTarget(target))
                continue;

            // 3. 조건(Condition) 확인 - 기술/하드코어 미션용
            if (!conditionChecker.check(player, template, context))
                continue;

            // 4. 진행도 업데이트
            // UserData.updateMissionProgress 내부에서 완료 체크까지 수행함
            userData.updateMissionProgress(data.getMissionId(), amount, template.getAmount());

            // 5. 실시간 진행 상황 알림 (Actionbar)
            sendProgressActionbar(player, template, data.getProgress());

            // 6. 완료 달성 시 처리
            if (data.getStatus() == MissionStatus.COMPLETED) {
                handleMissionCompletion(player, template, data);
            }
        }
    }

    /**
     * 진행 상황 액션바 출력
     */
    private void sendProgressActionbar(Player player, MissionTemplate template, int current) {
        // 예: [미션] 석탄 채굴: 5/10 (+1)
        String msg = String.format("§e[미션] §f%s: §a%d§7/§a%d",
                template.getDisplayName(), current, template.getAmount());
        player.sendTitle("", msg, 0, 40, 10);
    }

    /**
     * 미션 완료 처리 (보상 지급 전 단계)
     */
    private void handleMissionCompletion(Player player, MissionTemplate template, PlayerMissionData data) {
        // 성공 이펙트
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.sendTitle("§a미션 완료!", "§f" + template.getDisplayName(), 10, 70, 20);
        player.sendMessage("§a[미션] §f" + template.getDisplayName() + " §7완료! 보상이 지급됩니다.");

        // 자동 보상 지급
        completeMission(player, template.getId());
    }

    /**
     * 최종 보상 지급 및 연계(체인) 처리
     */
    public void completeMission(Player player, String missionId) {
        MissionTemplate template = getMission(missionId);
        if (template == null)
            return;

        com.dreamwork.core.UserData userData = plugin.getUserDataManager().getUserData(player);
        PlayerMissionData data = userData.getMission(missionId);

        if (data == null || data.getStatus() == MissionStatus.CLAIMED)
            return;

        // 1. 단순 보상 (돈, 아이템, 경험치)
        giveSimpleRewards(player, template);

        // 2. 복합 보상 (커맨드, 버프, 칭호 등)
        giveComplexRewards(player, template);

        // 3. 상태 변경 (CLAIMED)
        data.setStatus(MissionStatus.CLAIMED);

        // 4. 연계 미션(Chain/Next Tier) 자동 수락
        String nextMissionId = resolveNextMissionId(template);
        if (nextMissionId != null) {
            // 다음 미션 템플릿 존재 여부 확인
            if (missionCache.containsKey(nextMissionId)) {
                acceptMission(player, nextMissionId);
                player.sendMessage("§e[!] §f다음 단계 미션이 개방되었습니다!");
            }
        }
    }

    /**
     * 다음 미션 ID 찾기
     * next_mission 필드가 "tier_2" 처럼 되어있으면 "chainId_tier_2"로 변환
     */
    private String resolveNextMissionId(MissionTemplate template) {
        String next = template.getNextMission();
        if (next == null)
            return null;

        // 이미 전체 ID라면 그대로 반환
        if (missionCache.containsKey(next))
            return next;

        // 체인 시스템이라면 prefix 붙여서 시도
        if (template.getChainId() != null) {
            String chainedId = template.getChainId() + "_" + next;
            if (missionCache.containsKey(chainedId))
                return chainedId;
        }

        return null;
    }

    private void giveSimpleRewards(Player player, MissionTemplate template) {
        // 돈
        if (template.getRewardMoney() > 0) {
            plugin.getJobManager().giveMoney(player, template.getRewardMoney());
            player.sendMessage("§e💰 보상: §f" + template.getRewardMoney() + "G");
        }

        // 아이템
        for (String itemId : template.getRewardItems()) {
            org.bukkit.inventory.ItemStack item = plugin.getItemManager().createItem(itemId, 1);
            if (item != null) {
                player.getInventory().addItem(item);
            }
        }

        // 직업 경험치
        for (Map.Entry<String, Double> entry : template.getRewardJobExp().entrySet()) {
            JobType job = JobType.fromConfigKey(entry.getKey());
            if (job != null) {
                plugin.getJobManager().addExperience(player, job, entry.getValue());
                player.sendMessage("§e✨ 보상: §f" + job.getDisplayName() + " 경험치 +" + entry.getValue());
            }
        }
    }

    private void giveComplexRewards(Player player, MissionTemplate template) {
        // 커맨드 실행
        if (template.getRewardCommands() != null) {
            for (String cmd : template.getRewardCommands()) {
                String processed = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
            }
        }

        // 버프 (PotionEffect)
        if (template.getRewardBuffs() != null) {
            for (String buffStr : template.getRewardBuffs()) {
                try {
                    // FORMAT: EFFECT_TYPE:LEVEL:DURATION(seconds)
                    String[] parts = buffStr.split(":");
                    PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                    int amplifier = Integer.parseInt(parts[1]);
                    int duration = Integer.parseInt(parts[2]) * 20; // tick 변환

                    if (type != null) {
                        player.addPotionEffect(new PotionEffect(type, duration, amplifier));
                    }
                } catch (Exception e) {
                    plugin.log(Level.WARNING, "버프 보상 적용 실패: " + buffStr);
                }
            }
        }

        // 타이틀
        if (template.getRewardTitle() != null || template.getRewardSubtitle() != null) {
            player.sendTitle(
                    template.getRewardTitle() != null ? template.getRewardTitle() : "",
                    template.getRewardSubtitle() != null ? template.getRewardSubtitle() : "",
                    10, 70, 20);
        }
    }

    public MissionTemplate getMission(String id) {
        return missionCache.get(id);
    }

    public Collection<MissionTemplate> getAllMissions() {
        return missionCache.values();
    }

    /**
     * 모든 미션 ID 목록 (호환성 유지)
     */
    public Set<String> getAllMissionIds() {
        return new HashSet<>(missionCache.keySet());
    }
}
