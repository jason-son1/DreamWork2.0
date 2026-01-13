package com.dreamwork.hook;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.logging.Level;

/**
 * Towny 영토 시스템 Hook
 * 
 * Towny API를 통해 타운/영토 정보를 조회합니다.
 * Towny가 없어도 플러그인이 작동할 수 있도록 유연하게 처리합니다.
 * 
 * @author DreamWork Team
 */
public class TownyHook {

    private final DreamWorkPlugin plugin;
    private boolean enabled = false;

    public TownyHook(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    /**
     * Towny 연동 설정
     */
    private void setup() {
        if (Bukkit.getPluginManager().getPlugin("Towny") != null) {
            enabled = true;
            plugin.debug("Towny 플러그인 감지됨");
        } else {
            plugin.debug("Towny 플러그인을 찾을 수 없습니다 (선택적 기능)");
        }
    }

    /**
     * 연동 상태 확인
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 해당 위치가 타운 내부인지 확인
     */
    public boolean isInTown(Location location) {
        if (!enabled)
            return false;

        try {
            // Towny API를 사용하여 타운 블록 확인
            com.palmergames.bukkit.towny.object.TownBlock townBlock = com.palmergames.bukkit.towny.TownyAPI
                    .getInstance().getTownBlock(location);

            return townBlock != null && townBlock.hasTown();
        } catch (Exception e) {
            plugin.debug("Towny 조회 오류: " + e.getMessage());
            return false;
        }
    }

    /**
     * 해당 위치가 농경지(Farm Plot)인지 확인
     */
    public boolean isFarmPlot(Location location) {
        if (!enabled)
            return true; // Towny 없으면 제한 없음

        try {
            com.palmergames.bukkit.towny.object.TownBlock townBlock = com.palmergames.bukkit.towny.TownyAPI
                    .getInstance().getTownBlock(location);

            if (townBlock == null)
                return true; // 야생은 허용

            return townBlock.getType() == com.palmergames.bukkit.towny.object.TownBlockType.FARM;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 해당 위치가 상업 지구(Commercial)인지 확인
     */
    public boolean isCommercialPlot(Location location) {
        if (!enabled)
            return false;

        try {
            com.palmergames.bukkit.towny.object.TownBlock townBlock = com.palmergames.bukkit.towny.TownyAPI
                    .getInstance().getTownBlock(location);

            if (townBlock == null)
                return false;

            return townBlock.getType() == com.palmergames.bukkit.towny.object.TownBlockType.COMMERCIAL;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 해당 위치가 야생(Wilderness)인지 확인
     */
    public boolean isWilderness(Location location) {
        if (!enabled)
            return true;

        try {
            com.palmergames.bukkit.towny.object.TownBlock townBlock = com.palmergames.bukkit.towny.TownyAPI
                    .getInstance().getTownBlock(location);

            return townBlock == null || !townBlock.hasTown();
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 해당 위치의 타운 이름 가져오기
     */
    public String getTownName(Location location) {
        if (!enabled)
            return null;

        try {
            com.palmergames.bukkit.towny.object.TownBlock townBlock = com.palmergames.bukkit.towny.TownyAPI
                    .getInstance().getTownBlock(location);

            if (townBlock != null && townBlock.hasTown()) {
                return townBlock.getTownOrNull().getName();
            }
        } catch (Exception e) {
            plugin.debug("타운 이름 조회 오류: " + e.getMessage());
        }

        return null;
    }

    /**
     * 플레이어가 해당 타운의 주민인지 확인
     */
    public boolean isResident(org.bukkit.entity.Player player, Location location) {
        if (!enabled)
            return false;

        try {
            com.palmergames.bukkit.towny.object.TownBlock townBlock = com.palmergames.bukkit.towny.TownyAPI
                    .getInstance().getTownBlock(location);

            if (townBlock == null || !townBlock.hasTown())
                return false;

            com.palmergames.bukkit.towny.object.Resident resident = com.palmergames.bukkit.towny.TownyAPI.getInstance()
                    .getResident(player);

            if (resident == null || !resident.hasTown())
                return false;

            return resident.getTownOrNull().equals(townBlock.getTownOrNull());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 플레이어의 소속 타운 이름
     */
    public String getPlayerTown(org.bukkit.entity.Player player) {
        if (!enabled)
            return null;

        try {
            com.palmergames.bukkit.towny.object.Resident resident = com.palmergames.bukkit.towny.TownyAPI.getInstance()
                    .getResident(player);

            if (resident != null && resident.hasTown()) {
                return resident.getTownOrNull().getName();
            }
        } catch (Exception e) {
            plugin.debug("플레이어 타운 조회 오류: " + e.getMessage());
        }

        return null;
    }
}
