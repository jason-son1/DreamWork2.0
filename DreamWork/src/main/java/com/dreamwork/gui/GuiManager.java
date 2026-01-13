package com.dreamwork.gui;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.gui.impl.*;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * GUI 관리자
 * 
 * 모든 GUI 생성 및 열기를 관리합니다.
 * 
 * @author DreamWork Team
 */
public class GuiManager {

    private final DreamWorkPlugin plugin;

    public GuiManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 메인 대시보드 열기
     */
    public void openMainDashboard(Player player) {
        try {
            MainDashboardGui gui = new MainDashboardGui(plugin, player);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: MainDashboard - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 직업 상세 정보 GUI 열기
     */
    public void openJobDetailGui(Player player, com.dreamwork.job.JobType jobType) {
        try {
            JobDetailGui gui = new JobDetailGui(plugin, player, jobType);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: JobDetail - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 대장간 GUI 열기
     */
    public void openForgeGui(Player player) {
        try {
            ForgeGui gui = new ForgeGui(plugin, player);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: Forge - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 주방 GUI 열기
     */
    public void openKitchenGui(Player player) {
        try {
            KitchenGui gui = new KitchenGui(plugin, player);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: Kitchen - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 어시장 GUI 열기
     */
    public void openFishMarketGui(Player player) {
        try {
            FishMarketGui gui = new FishMarketGui(plugin, player);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: FishMarket - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 미션 게시판 GUI 열기
     */
    public void openMissionBoardGui(Player player) {
        try {
            MissionBoardGui gui = new MissionBoardGui(plugin, player);
            gui.open();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "GUI 열기 실패: MissionBoard - " + e.getMessage());
            player.sendMessage("§c메뉴를 여는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 설정 리로드
     */
    public void reload() {
        // GUI 설정 리로드 (필요시)
        plugin.debug("GUI 매니저 리로드 완료");
    }
}
