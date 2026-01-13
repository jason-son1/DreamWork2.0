package com.dreamwork.mission;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

/**
 * 미션 이벤트 리스너
 * 
 * 공통 미션(생활, 경제, 사회)과 관련된 이벤트를 처리합니다.
 * 접속 시 초기화 로직도 담당합니다.
 * 
 * @author DreamWork Team
 */
public class MissionEventListener implements Listener {

    private final DreamWorkPlugin plugin;

    public MissionEventListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 접속 시 일일/주간 초기화 체크
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UserData userData = plugin.getUserDataManager().getUserData(player);

        long now = System.currentTimeMillis();

        // 일일 초기화 (매일 자정)
        long lastDaily = userData.getLastDailyReset();
        if (isNextDay(lastDaily, now)) {
            resetDailyMissions(player, userData);
            userData.setLastDailyReset(now);
            player.sendMessage("§e[정보] §f일일 미션이 초기화되었습니다.");
        }

        // 주간 초기화 (매주 월요일)
        long lastWeekly = userData.getLastWeeklyReset();
        if (isNextWeek(lastWeekly, now)) {
            resetWeeklyMissions(player, userData);
            userData.setLastWeeklyReset(now);
            player.sendMessage("§e[정보] §f주간 미션이 초기화되었습니다.");
        }

        // [출석] 미션 자동 체크
        plugin.getMissionManager().processEvent(player, MissionType.STATISTIC_ACCUMULATE, "LOGIN", 1);
    }

    /**
     * 아이템 섭취 (음식 등)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        String materialName = event.getItem().getType().name();

        plugin.getMissionManager().processEvent(player, MissionType.EAT, materialName, 1);
    }

    /**
     * 채팅 (소통 미션)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // 비동기 이벤트 -> 메인 스레드 호출
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getMissionManager().processEvent(player, MissionType.CHAT_CHANNEL, "GLOBAL", 1);
        });
    }

    /**
     * 이동 (통계 기반)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMoveStat(PlayerStatisticIncrementEvent event) {
        if (event.getStatistic() == Statistic.WALK_ONE_CM || event.getStatistic() == Statistic.SPRINT_ONE_CM) {
            int newValue = event.getNewValue();
            int oldValue = event.getPreviousValue();

            // 10블록(1000cm) 이동 시마다 체크
            if (newValue / 1000 > oldValue / 1000) {
                plugin.getMissionManager().processEvent(event.getPlayer(), MissionType.WALK, "ANY", 10);
            }
        }
    }

    /**
     * 사망 (생존 미션 등)
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // 필요 시 구현
    }

    // ==================== 초기화 로직 ====================

    private void resetDailyMissions(Player player, UserData userData) {
        for (MissionTemplate template : plugin.getMissionManager().getAllMissions()) {
            if ("DAILY".equalsIgnoreCase(template.getResetCycle())) {
                userData.resetMission(template.getId());
                plugin.getMissionManager().acceptMission(player, template.getId());
            }
        }
    }

    private void resetWeeklyMissions(Player player, UserData userData) {
        for (MissionTemplate template : plugin.getMissionManager().getAllMissions()) {
            if ("WEEKLY".equalsIgnoreCase(template.getResetCycle())) {
                userData.resetMission(template.getId());
                plugin.getMissionManager().acceptMission(player, template.getId());
            }
        }
    }

    private boolean isNextDay(long lastReset, long now) {
        if (lastReset == 0)
            return true;
        // 24시간 = 86400000ms
        // 더 정확하게 하려면 Calendar나 LocalDateTime 사용
        return (now - lastReset) > 86400000L;
    }

    private boolean isNextWeek(long lastReset, long now) {
        if (lastReset == 0)
            return true;
        return (now - lastReset) > 604800000L;
    }
}
