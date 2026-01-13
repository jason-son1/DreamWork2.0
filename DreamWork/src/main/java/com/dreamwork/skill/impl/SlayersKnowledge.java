package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 사냥꾼의 지식 스킬 (사냥꾼)
 * 
 * 몬스터 타입에 따라 추가 데미지를 입힙니다.
 * 
 * 패시브 스킬로, 해금 후 자동 적용됩니다.
 * 
 * 해금 레벨: 20
 * 기본 추가 데미지: 10%
 * 
 * @author DreamWork Team
 */
public class SlayersKnowledge implements Listener {

    private final DreamWorkPlugin plugin;
    private static final String SKILL_ID = "slayers_knowledge";

    public SlayersKnowledge(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 엔티티 데미지 이벤트 - 추가 데미지
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 플레이어가 공격자인지 확인
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        // 몬스터 대상인지 확인
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        // 스킬 해금 확인
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int level = userData.getJobLevel(JobType.HUNTER);
        int unlockLevel = plugin.getSkillManager().getSkillUnlockLevel(JobType.HUNTER, SKILL_ID);

        if (level < unlockLevel) {
            return;
        }

        // 설정된 보너스 가져오기 (기본 10%)
        double bonus = plugin.getConfigManager().getJobConfig("hunter")
                .getDouble("skills.slayers_knowledge.bonus-damage", 0.1);

        // 레벨 보너스 (20레벨마다 1% 추가 예시)
        double levelBonus = (level - unlockLevel) * 0.001;

        double totalBonus = bonus + levelBonus;

        double damage = event.getDamage();
        double addedDamage = damage * totalBonus;

        event.setDamage(damage + addedDamage);

        // 디버그 (너무 빈도가 높을 수 있으므로 주석 처리 또는 디버그 레벨 조정)
        // plugin.debug("Slayer's Knowledge: +" + String.format("%.1f", addedDamage));
    }
}
