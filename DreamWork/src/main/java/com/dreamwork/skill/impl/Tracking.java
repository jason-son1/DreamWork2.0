package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import com.dreamwork.skill.SkillManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 추적 스킬 (사냥꾼)
 * 
 * 주변의 몬스터(특히 강력한 개체)를 나침반으로 추적합니다.
 * 
 * 해금 레벨: 30
 * 쿨다운: 5분
 * 
 * @author DreamWork Team
 */
public class Tracking {

    private final DreamWorkPlugin plugin;
    private static final String SKILL_ID = "tracking";

    public Tracking(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 스킬 사용
     */
    public boolean use(Player player) {
        SkillManager skillManager = plugin.getSkillManager();

        // 사용 가능 여부 확인
        if (!skillManager.canUseSkill(player, JobType.HUNTER, SKILL_ID)) {
            return false;
        }

        // 스킬 설정 가져오기
        int range = plugin.getConfigManager().getJobConfig("hunter")
                .getInt("skills.tracking.range", 100);
        int cooldown = skillManager.getSkillCooldown(JobType.HUNTER, SKILL_ID);

        // 주변 몬스터 탐색
        Monster target = findTarget(player, range);

        if (target == null) {
            player.sendMessage("§7주변에 추적할 대상이 없습니다.");
            return false; // 쿨다운 적용 안 함
        }

        // 나침반 설정
        player.setCompassTarget(target.getLocation());

        String targetName = target.getCustomName() != null ? target.getCustomName() : target.getType().name();
        player.sendMessage("§a🎯 추적 시작! 대상: " + targetName +
                " (거리: " + (int) player.getLocation().distance(target.getLocation()) + "m)");

        // 사운드
        player.playSound(player.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, 1.0f, 1.2f);

        // 쿨다운 시작
        skillManager.startCooldown(player, SKILL_ID, cooldown);

        return true;
    }

    /**
     * 추적 대상 찾기 (보스 > 엘리트 > 일반)
     */
    private Monster findTarget(Player player, int range) {
        List<Entity> nearby = player.getNearbyEntities(range, range, range);

        // 몬스터만 필터링
        List<Monster> monsters = nearby.stream()
                .filter(e -> e instanceof Monster)
                .map(e -> (Monster) e)
                .collect(Collectors.toList());

        if (monsters.isEmpty())
            return null;

        // 1순위: 보스급 (Wither, Dragon, Warden)
        Monster boss = monsters.stream()
                .filter(m -> isBoss(m))
                .min(Comparator.comparingDouble(m -> m.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);

        if (boss != null)
            return boss;

        // 2순위: 이름이 있는 몬스터 (엘리트 추정)
        Monster elite = monsters.stream()
                .filter(m -> m.getCustomName() != null)
                .min(Comparator.comparingDouble(m -> m.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);

        if (elite != null)
            return elite;

        // 3순위: 가장 가까운 일반 몬스터
        return monsters.stream()
                .min(Comparator.comparingDouble(m -> m.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);
    }

    private boolean isBoss(Monster m) {
        switch (m.getType()) {
            case WITHER:
            case ENDER_DRAGON:
            case WARDEN:
            case ELDER_GUARDIAN:
                return true;
            default:
                return false;
        }
    }
}
