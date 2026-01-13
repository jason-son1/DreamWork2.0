package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * 약점 간파 스킬 (사냥꾼)
 * 
 * 몬스터를 공격할 때 일정 확률로 치명타를 터뜨리고,
 * 대상에게 발광(Glowing) 효과를 부여합니다.
 * 
 * 패시브 스킬로, 해금 후 자동 적용됩니다.
 * 
 * 해금 레벨: 15
 * 기본 확률: 15%
 * 추가 데미지: 50%
 * 
 * @author DreamWork Team
 */
public class CriticalEye implements Listener {

    private final DreamWorkPlugin plugin;
    private final Random random = new Random();
    private static final String SKILL_ID = "critical_eye";

    public CriticalEye(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 엔티티 데미지 이벤트 - 치명타 처리
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 플레이어가 공격자인지 확인
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        // 몬스터 대상인지 확인
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }

        // 스킬 해금 확인
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int level = userData.getJobLevel(JobType.HUNTER);
        int unlockLevel = plugin.getSkillManager().getSkillUnlockLevel(JobType.HUNTER, SKILL_ID);

        if (level < unlockLevel) {
            return;
        }

        // 확률 계산 (레벨당 0.5% 추가)
        double baseChance = plugin.getConfigManager().getJobConfig("hunter")
                .getDouble("skills.critical_eye.chance", 0.15);
        double levelBonus = (level - unlockLevel) * 0.005;
        double finalChance = Math.min(0.5, baseChance + levelBonus); // 최대 50%

        if (random.nextDouble() > finalChance) {
            return;
        }

        // 치명타 발동!
        double damageMultiplier = plugin.getConfigManager().getJobConfig("hunter")
                .getDouble("skills.critical_eye.damage_multiplier", 1.5);

        double originalDamage = event.getDamage();
        double criticalDamage = originalDamage * damageMultiplier;
        event.setDamage(criticalDamage);

        // 발광 효과 부여
        monster.addPotionEffect(new PotionEffect(
                PotionEffectType.GLOWING,
                100, // 5초
                0,
                false,
                false,
                true));

        // 파티클 효과
        player.spawnParticle(
                Particle.CRIT,
                monster.getLocation().add(0, 1, 0),
                15,
                0.5, 0.5, 0.5,
                0.1);

        // 사운드
        player.playSound(monster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);

        // 메시지
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        "§c⚔ 약점 간파! §f" + String.format("%.1f", criticalDamage) + " 데미지"));

        plugin.debug(player.getName() + " 약점 간파 발동: " +
                String.format("%.1f -> %.1f", originalDamage, criticalDamage));
    }
}
