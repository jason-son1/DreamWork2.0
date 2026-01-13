package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * 입질 감지 스킬 (어부)
 * 
 * 물고기가 미끼를 물었을 때 효과음과 파티클로 알려줍니다.
 * 패시브 스킬로, 해금 후 자동 적용됩니다.
 * 
 * 해금 레벨: 10
 * 
 * @author DreamWork Team
 */
public class Sensitivity implements Listener {

    private final DreamWorkPlugin plugin;
    private static final String SKILL_ID = "sensitivity";

    public Sensitivity(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 낚시 이벤트 - 입질 감지 및 대기 시간 단축
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        // 1. 낚시 시작 (대기 시간 단축)
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            handleFishingStart(player, event.getHook());
            return;
        }

        // 2. 입질 감지 (시각/청각 효과)
        if (event.getState() == PlayerFishEvent.State.BITE) {
            handleBite(player, event.getHook());
        }
    }

    private void handleFishingStart(Player player, org.bukkit.entity.FishHook hook) {
        // 스킬 해금 확인
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int level = userData.getJobLevel(JobType.FISHER);
        int unlockLevel = plugin.getSkillManager().getSkillUnlockLevel(JobType.FISHER, SKILL_ID);

        if (level < unlockLevel) {
            return;
        }

        // 감소량 계산 (Config: base + rank bonus)
        int baseReduction = plugin.getConfigManager().getJobConfig("fisher")
                .getInt("skills.sensitivity.base-reduction", 20);
        int reductionPerRank = plugin.getConfigManager().getJobConfig("fisher")
                .getInt("skills.sensitivity.reduction-per-rank", 20);

        // 간단히 레벨 10마다 랭크 1 증가로 계산
        int rank = (level - unlockLevel) / 10;
        int totalReduction = baseReduction + (rank * reductionPerRank);

        // 기존 대기 시간에서 차감 (기본값: Min 100, Max 600)
        // 1.16+ API 필요 (setMinWaitTime, setMaxWaitTime)
        try {
            int currentMin = hook.getMinWaitTime();
            int currentMax = hook.getMaxWaitTime();

            hook.setMinWaitTime(Math.max(20, currentMin - totalReduction));
            hook.setMaxWaitTime(Math.max(40, currentMax - totalReduction));

            // plugin.debug(player.getName() + " 민감도 발동: 대기 시간 -" + totalReduction + "틱");
        } catch (NoSuchMethodError e) {
            // 구버전 호환성 (Lure 인챈트로 대체하거나 무시)
        }
    }

    private void handleBite(Player player, org.bukkit.entity.FishHook hook) {
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int level = userData.getJobLevel(JobType.FISHER);

        // 레벨에 따라 효과 강도 조절
        float volume = Math.min(1.5f, 0.8f + (level * 0.01f));
        int particleCount = Math.min(20, 5 + (level / 10));

        // 사운드 효과
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, volume, 1.5f);

        // 파티클 효과 (찌 위치에)
        if (hook != null) {
            player.spawnParticle(
                    Particle.BUBBLE_POP,
                    hook.getLocation(),
                    particleCount,
                    0.3, 0.1, 0.3,
                    0.05);
        }

        // 액션바 알림
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§b🎣 입질! 지금 당기세요!"));
    }
}
