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
     * 낚시 이벤트 - 물기 감지
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        // 물고기가 물었을 때만 처리
        if (event.getState() != PlayerFishEvent.State.BITE) {
            return;
        }

        Player player = event.getPlayer();

        // 스킬 해금 확인
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int level = userData.getJobLevel(JobType.FISHER);
        int unlockLevel = plugin.getSkillManager().getSkillUnlockLevel(JobType.FISHER, SKILL_ID);

        if (level < unlockLevel) {
            return;
        }

        // 레벨에 따라 효과 강도 조절
        float volume = Math.min(1.5f, 0.8f + (level * 0.01f));
        int particleCount = Math.min(20, 5 + (level / 10));

        // 사운드 효과
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, volume, 1.5f);

        // 파티클 효과 (찌 위치에)
        if (event.getHook() != null) {
            player.spawnParticle(
                    Particle.BUBBLE_POP,
                    event.getHook().getLocation(),
                    particleCount,
                    0.3, 0.1, 0.3,
                    0.05);
        }

        // 액션바 알림
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§b🎣 입질! 지금 당기세요!"));

        plugin.debug(player.getName() + " 입질 감지 발동");
    }
}
