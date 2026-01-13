package com.dreamwork.mission;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Calendar;
import java.util.List;

/**
 * 미션 시스템 리스너
 * 
 * 플레이어 접속 시 미션 할당/초기화 등을 처리합니다.
 * 
 * @author DreamWork Team
 */
public class MissionListener implements Listener {

    private final DreamWorkPlugin plugin;

    public MissionListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UserData userData = plugin.getUserDataManager().getUserData(player);

        checkAndResetMissions(player, userData);
        assignMissions(player, userData);
    }

    /**
     * 일일/주간 미션 초기화 체크
     */
    private void checkAndResetMissions(Player player, UserData userData) {
        long now = System.currentTimeMillis();
        long lastDaily = userData.getLastDailyReset();
        long lastWeekly = userData.getLastWeeklyReset();

        // 일일 초기화 (오전 6시 기준)
        if (isResetNeeded(lastDaily, now, Calendar.DATE)) {
            userData.setLastDailyReset(now);
            resetMissionsByCycle(userData, "DAILY");
            player.sendMessage("§e[미션] §f일일 미션이 초기화되었습니다.");
        }

        // 주간 초기화 (월요일 오전 6시 기준)
        if (isResetNeeded(lastWeekly, now, Calendar.WEEK_OF_YEAR)) {
            userData.setLastWeeklyReset(now);
            resetMissionsByCycle(userData, "WEEKLY");
            player.sendMessage("§e[미션] §f주간 미션이 초기화되었습니다.");
        }
    }

    /**
     * 특정 주기의 미션 삭제
     */
    private void resetMissionsByCycle(UserData userData, String cycle) {
        List<String> missionsToRemove = userData.getAllMissions().values().stream()
                .filter(data -> {
                    MissionTemplate template = plugin.getMissionManager().getMission(data.getMissionId());
                    return template != null && cycle.equalsIgnoreCase(template.getResetCycle());
                })
                .map(PlayerMissionData::getMissionId)
                .toList();

        for (String id : missionsToRemove) {
            userData.resetMission(id);
        }
    }

    /**
     * 미션 할당
     */
    private void assignMissions(Player player, UserData userData) {
        for (String missionId : plugin.getMissionManager().getAllMissionIds()) {
            MissionTemplate template = plugin.getMissionManager().getMission(missionId);
            if (template == null)
                continue;

            // 이미 있는 미션은 패스
            if (userData.getMission(missionId) != null)
                continue;

            // 자동 할당 미션인지 확인 (DAILY, WEEKLY는 자동 할당)
            String cycle = template.getResetCycle();
            if ("DAILY".equalsIgnoreCase(cycle) || "WEEKLY".equalsIgnoreCase(cycle)) {
                plugin.getMissionManager().acceptMission(player, missionId);
            }

            // ONE_TIME 미션 중 조건 만족 시 자동 수락 로직은 여기에 추가 가능
        }
    }

    /**
     * 초기화 필요 여부 확인
     */
    private boolean isResetNeeded(long lastResetTime, long now, int calendarField) {
        if (lastResetTime == 0)
            return true;

        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(lastResetTime);

        // 6시 이전이면 하루 전으로 취급
        if (last.get(Calendar.HOUR_OF_DAY) < 6) {
            last.add(Calendar.DATE, -1);
        }

        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(now);

        if (current.get(Calendar.HOUR_OF_DAY) < 6) {
            current.add(Calendar.DATE, -1);
        }

        return last.get(calendarField) != current.get(calendarField)
                || last.get(Calendar.YEAR) != current.get(Calendar.YEAR);
    }
}
