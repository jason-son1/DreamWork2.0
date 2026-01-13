package com.dreamwork.gui.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.gui.DreamGui;
import com.dreamwork.job.JobType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 메인 대시보드 GUI
 * 
 * 플레이어의 상태 요약 및 5대 직업으로의 네비게이션을 제공합니다.
 * 
 * 레이아웃 (27슬롯):
 * [ 00 ][ 01 ][ 02 ][ 03 ][ 04 ][ 05 ][ 06 ][ 07 ][ 08 ] <- 장식 (유리판)
 * [ 09 ][ 10 ][ 11 ][ 12 ][ 13 ][ 14 ][ 15 ][ 16 ][ 17 ] <- 직업 & 프로필 핵심 라인
 * [ 18 ][ 19 ][ 20 ][ 21 ][ 22 ][ 23 ][ 24 ][ 25 ][ 26 ] <- 기능 & 미션 라인
 * 
 * @author DreamWork Team
 */
public class MainDashboardGui extends DreamGui {

    private final UserData userData;

    public MainDashboardGui(DreamWorkPlugin plugin, Player player) {
        super(plugin, player, "§6§lDreamWork §f: §7나의 상태", 3); // 3행 = 27슬롯
        this.userData = plugin.getUserDataManager().getUserData(player);
    }

    @Override
    public void initialize() {
        // 배경 유리판으로 채우기
        ItemStack backgroundGlass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, backgroundGlass);
        }
        for (int i = 18; i < 27; i++) {
            if (i != 21 && i != 22 && i != 23) { // 하단 기능 버튼 슬롯 제외
                inventory.setItem(i, backgroundGlass);
            }
        }
        inventory.setItem(9, backgroundGlass);
        inventory.setItem(17, backgroundGlass);

        // 중앙: 플레이어 프로필 (Slot 13)
        inventory.setItem(13, createProfileItem());

        // 좌측: 생산 직업군 (Slot 10, 11, 12)
        inventory.setItem(10, createJobIcon(JobType.MINER));
        inventory.setItem(11, createJobIcon(JobType.FARMER));
        inventory.setItem(12, createJobIcon(JobType.FISHER));

        // 우측: 탐험/전투 직업군 (Slot 14, 15)
        inventory.setItem(14, createJobIcon(JobType.HUNTER));
        inventory.setItem(15, createJobIcon(JobType.ADVENTURER));

        // 하단: 기능 버튼
        inventory.setItem(21, createRankingIcon());
        inventory.setItem(22, createMissionBoardIcon());
        inventory.setItem(23, createExchangeIcon());
    }

    /**
     * 플레이어 프로필 아이템 생성
     */
    private ItemStack createProfileItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        meta.setOwningPlayer(player);
        meta.setDisplayName("§e§l[ " + player.getName() + " 님의 여권 ]");

        List<String> lore = new ArrayList<>();
        lore.add("§7DreamWork ID: §f#" + player.getUniqueId().toString().substring(0, 8));
        lore.add("§7");
        lore.add("§f🎖️ 시민 등급: §b거주민"); // TODO: 시민 등급 시스템 구현 후 연동
        lore.add("§f💰 보유 자산: §e" + plugin.getJobManager().getFormattedBalance(player) + " D");
        lore.add("§7");
        lore.add("§7⏲️ 평균 레벨: §f" + String.format("%.1f", userData.getAverageLevel()));
        lore.add("§7📊 총 레벨: §f" + userData.getTotalLevels());
        lore.add("§7");
        lore.add("§e클릭하여 시민 등급 혜택 보기");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 직업 아이콘 생성
     */
    private ItemStack createJobIcon(JobType jobType) {
        int level = userData.getJobLevel(jobType);
        double exp = userData.getJobExp(jobType);
        double required = plugin.getJobManager().getRequiredExp(level);
        double percent = (exp / required) * 100;

        // 레벨에 따라 재질 변경
        Material material = getJobMaterial(jobType, level);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(jobType.getColorCode() + "[ " + jobType.getIcon() + " " +
                jobType.getDisplayName() + " : " + jobType.getSubtitle() + " ]");

        List<String> lore = new ArrayList<>();
        lore.add("§7레벨: §fLv." + level);
        lore.add("§7숙련도: " + getProgressBar(percent / 100, 10) +
                " §7(" + String.format("%.1f", percent) + "%)");
        lore.add("§7");

        // 현재 진행 중인 미션 표시 (TODO: 미션 시스템 연동)
        lore.add("§f📜 현재 임무: §7미구현");
        lore.add("§7");

        // 대표 스킬 표시
        lore.add("§7⚒️ 대표 스킬: " + getMainSkillName(jobType));
        lore.add("§7");
        lore.add("§e클릭하여 " + jobType.getDisplayName() + " 전용 메뉴 열기");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 랭킹 아이콘 생성
     */
    private ItemStack createRankingIcon() {
        List<String> lore = List.of(
                "§7서버 내 전체 직업 랭킹과",
                "§7나의 종합 순위를 확인합니다.",
                "§7",
                "§c🔥 준비 중...");
        return createItem(Material.NETHER_STAR, "§e[ 랭킹 및 통계 ]", lore);
    }

    /**
     * 미션 보드 아이콘 생성
     */
    private ItemStack createMissionBoardIcon() {
        List<String> lore = List.of(
                "§7직업별 미션 외에",
                "§7서버 공통 미션(시민 의무)을 확인합니다.",
                "§7",
                "§f📅 일일 미션: §e준비 중",
                "§f📅 주간 미션: §e준비 중",
                "§7",
                "§e클릭하여 미션 보드 열기");
        return createItem(Material.WRITABLE_BOOK, "§a[ 📋 통합 미션 보드 ]", lore);
    }

    /**
     * 통합 교환소 아이콘 생성
     */
    private ItemStack createExchangeIcon() {
        List<String> lore = List.of(
                "§7각 직업의 결과물(광석, 농작물, 물고기 등)을",
                "§7교환하거나 가공소로 바로 이동할 수 있습니다.",
                "§7",
                "§c🔥 준비 중...");
        return createItem(Material.ENDER_CHEST, "§6[ ⚖️ 통합 교환소 ]", lore);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getSlot();
        Player clicker = (Player) event.getWhoClicked();

        // 직업 아이콘 클릭
        if (slot == 10) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openJobDetailGui(clicker, JobType.MINER);
        } else if (slot == 11) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openJobDetailGui(clicker, JobType.FARMER);
        } else if (slot == 12) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openJobDetailGui(clicker, JobType.FISHER);
        } else if (slot == 14) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openJobDetailGui(clicker, JobType.HUNTER);
        } else if (slot == 15) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openJobDetailGui(clicker, JobType.ADVENTURER);
        }
        // 미션 보드
        else if (slot == 22) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openMissionBoardGui(clicker);
        }
        // 기타 슬롯은 아무 동작 없음 (준비 중)
    }

    /**
     * 레벨에 따른 직업 아이콘 재질 결정
     */
    private Material getJobMaterial(JobType jobType, int level) {
        return switch (jobType) {
            case MINER -> {
                if (level < 10)
                    yield Material.STONE_PICKAXE;
                if (level < 30)
                    yield Material.IRON_PICKAXE;
                if (level < 50)
                    yield Material.DIAMOND_PICKAXE;
                yield Material.NETHERITE_PICKAXE;
            }
            case FARMER -> {
                if (level < 20)
                    yield Material.WOODEN_HOE;
                if (level < 40)
                    yield Material.IRON_HOE;
                yield Material.GOLDEN_HOE;
            }
            case FISHER -> Material.FISHING_ROD;
            case HUNTER -> {
                if (level < 30)
                    yield Material.BOW;
                yield Material.CROSSBOW;
            }
            case ADVENTURER -> Material.FILLED_MAP;
        };
    }

    /**
     * 직업별 대표 스킬 이름
     */
    private String getMainSkillName(JobType jobType) {
        return switch (jobType) {
            case MINER -> "광맥 탐지, 합금 제련";
            case FARMER -> "녹색 손길, 요리";
            case FISHER -> "입질 감지, 납품";
            case HUNTER -> "약점 간파, 도감";
            case ADVENTURER -> "육감, 지도 제작";
        };
    }

    /**
     * 프로그레스 바 생성
     */
    private String getProgressBar(double percent, int totalBars) {
        int filledBars = (int) (percent * totalBars);
        return "§a" + "█".repeat(Math.max(0, filledBars)) +
                "§7" + "█".repeat(Math.max(0, totalBars - filledBars));
    }
}
