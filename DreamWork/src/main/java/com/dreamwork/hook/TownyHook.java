package com.dreamwork.hook;

import com.dreamwork.DreamWorkPlugin;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Towny 플러그인 연동 훅
 * 
 * 타운, 플롯 정보를 가져옵니다.
 * 
 * @author DreamWork Team
 */
public class TownyHook {

    private final DreamWorkPlugin plugin;
    private final boolean isEnabled;

    public TownyHook(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        this.isEnabled = plugin.getServer().getPluginManager().getPlugin("Towny") != null;

        if (isEnabled) {
            plugin.log(Level.INFO, "Towny 플러그인이 감지되었습니다. 연동을 활성화합니다.");
        } else {
            plugin.log(Level.WARNING, "Towny 플러그인을 찾을 수 없습니다. 타운 관련 기능이 비활성화됩니다.");
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * 플레이어가 자신의 타운 안에 있는지 확인
     */
    public boolean isInOwnTown(Player player, Location location) {
        if (!isEnabled)
            return true; // Towny가 없으면 항상 true (제약 없음)

        try {
            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
            if (townBlock == null || !townBlock.hasTown())
                return false;

            Town town = townBlock.getTown();
            return town.hasResident(player.getName());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 특정 플롯 타입인지 확인
     */
    public boolean isPlotType(Location location, TownBlockType type) {
        if (!isEnabled)
            return false;

        try {
            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
            if (townBlock == null)
                return false;

            return townBlock.getType() == type;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 농장 구역인지 확인
     */
    public boolean isFarmPlot(Location location) {
        return isPlotType(location, TownBlockType.FARM);
    }

    /**
     * 해당 위치의 타운 이름 가져오기
     */
    public String getTownName(Location location) {
        if (!isEnabled)
            return null;

        try {
            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
            if (townBlock != null && townBlock.hasTown()) {
                return townBlock.getTown().getName();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * 타운 은행에 입금
     * 주의: Towny 버전에 따라 API가 다를 수 있음
     */
    public boolean depositToTown(Player player, double amount) {
        if (!isEnabled)
            return false;

        try {
            com.palmergames.bukkit.towny.object.Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null || !resident.hasTown())
                return false;

            Town town = resident.getTown();
            // Towny 최신 API 기준
            town.getAccount().deposit(amount, "DreamWork Deposit");
            return true;
        } catch (Exception e) {
            plugin.log(Level.WARNING, "타운 입금 실패: " + e.getMessage());
            return false;
        }
    }
}
