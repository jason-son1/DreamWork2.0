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

import java.util.ArrayList;
import java.util.List;

/**
 * 직업 상세 정보 GUI
 * 
 * 각 직업의 상세 정보, 스킬, 통계를 보여줍니다.
 * 
 * @author DreamWork Team
 */
public class JobDetailGui extends DreamGui {

    private final DreamWorkPlugin plugin;
    private final JobType jobType;

    public JobDetailGui(DreamWorkPlugin plugin, Player player, JobType jobType) {
        super(plugin, player, getTitle(plugin, jobType), 5);
        this.plugin = plugin;
        this.jobType = jobType;
    }

    private static String getTitle(DreamWorkPlugin plugin, JobType jobType) {
        String jobName = plugin.getConfigManager().getMessage("job-names." + jobType.name());
        return plugin.getConfigManager().getMessage("gui.job-detail-title")
                .replace("{job}", jobName);
    }

    @Override
    public void initialize() {
        UserData userData = plugin.getUserDataManager().getUserData(player);

        // 배경 채우기
        ItemStack glass = createGlassPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, glass);
        }

        // 상단 중앙: 직업 대표 아이콘
        inventory.setItem(4, createJobMainIcon(userData));

        // 스킬 슬롯 (2열)
        inventory.setItem(11, createSkillIcon("skill_1", 10));
        inventory.setItem(13, createSkillIcon("skill_2", 25));
        inventory.setItem(15, createSkillIcon("skill_3", 50));

        // 통계 슬롯 (4열)
        inventory.setItem(30, createStatIcon1(userData));
        inventory.setItem(32, createStatIcon2(userData));

        // 하단: 기능 버튼
        inventory.setItem(36, createBackButton()); // 뒤로 가기
        inventory.setItem(40, createFacilityButton()); // 시설 이동
        inventory.setItem(44, createHelpButton()); // 도움말
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getSlot();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

        switch (slot) {
            case 36 -> plugin.getGuiManager().openMainDashboard(player);
            case 40 -> openFacility();
        }
    }

    /**
     * 직업 시설 열기
     */
    private void openFacility() {
        switch (jobType) {
            case MINER -> plugin.getGuiManager().openForgeGui(player);
            case FARMER -> plugin.getGuiManager().openKitchenGui(player);
            case FISHER -> plugin.getGuiManager().openFishMarketGui(player);
            default -> player.sendMessage("§e이 직업의 전용 시설은 아직 개발 중입니다.");
        }
    }

    /**
     * 직업 메인 아이콘 생성
     */
    private ItemStack createJobMainIcon(UserData userData) {
        int level = userData.getJobLevel(jobType);
        Material material = getJobMaterial(level);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String jobName = plugin.getConfigManager().getMessage("job-names." + jobType.name());
        meta.setDisplayName(getJobColor() + "§l" + jobType.getIcon() + " " + jobName);

        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§f레벨: §b" + level + " / " + plugin.getJobManager().getMaxLevel());

        double exp = userData.getJobExp(jobType);
        double required = plugin.getJobManager().getRequiredExp(level);
        double percent = (exp / required) * 100;

        lore.add("§f경험치: §e" + String.format("%,.0f", exp) + " / " + String.format("%,.0f", required));
        lore.add("§f진행률: " + createProgressBar(percent));
        lore.add("§7");
        lore.add("§f총 누적 경험치: §a" + String.format("%,.0f", userData.getTotalExp(jobType)));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 스킬 아이콘 생성
     */
    private ItemStack createSkillIcon(String skillId, int unlockLevel) {
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int currentLevel = userData.getJobLevel(jobType);
        boolean unlocked = currentLevel >= unlockLevel;

        Material material = unlocked ? Material.ENCHANTED_BOOK : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (unlocked) {
            meta.setDisplayName("§a[해금됨] 스킬 #" + skillId);
            List<String> lore = new ArrayList<>();
            lore.add("§7스킬 설명...");
            lore.add("§7");
            lore.add("§a✔ 사용 가능");
            meta.setLore(lore);
        } else {
            meta.setDisplayName("§c[잠김] ???");
            List<String> lore = new ArrayList<>();
            lore.add("§7해금 조건:");
            lore.add("§e- " + getJobName() + " Lv." + unlockLevel + " 달성");
            lore.add("§7");
            lore.add("§c✘ 해금되지 않음");
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 통계 아이콘 1
     */
    private ItemStack createStatIcon1(UserData userData) {
        ItemStack item = new ItemStack(getMaterialForStat1());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e통계 정보 1");

        List<String> lore = new ArrayList<>();
        lore.add("§7이 직업에서의 활동 통계");
        lore.add("§7");
        lore.add("§7(세부 통계는 추후 구현)");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 통계 아이콘 2
     */
    private ItemStack createStatIcon2(UserData userData) {
        ItemStack item = new ItemStack(getMaterialForStat2());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e통계 정보 2");

        meta.setLore(List.of("§7(세부 통계는 추후 구현)"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 뒤로 가기 버튼
     */
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c[ ← 뒤로 가기 ]");
        meta.setLore(List.of("§7메인 대시보드로 돌아갑니다."));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 시설 이동 버튼
     */
    private ItemStack createFacilityButton() {
        Material material = switch (jobType) {
            case MINER -> Material.ANVIL;
            case FARMER -> Material.SMOKER;
            case FISHER -> Material.PUFFERFISH_BUCKET;
            case HUNTER -> Material.SKELETON_SKULL;
            case ADVENTURER -> Material.WRITABLE_BOOK;
        };

        String facilityName = switch (jobType) {
            case MINER -> "대장간";
            case FARMER -> "주방";
            case FISHER -> "어시장";
            case HUNTER -> "현상수배 게시판";
            case ADVENTURER -> "탐사 보고";
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b[ " + facilityName + " 이동 ]");
        meta.setLore(List.of("§e클릭하여 이동"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 도움말 버튼
     */
    private ItemStack createHelpButton() {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e[ ? 도움말 ]");
        meta.setLore(List.of("§7이 직업에 대한 가이드를 봅니다."));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 유리판 생성
     */
    private ItemStack createGlassPane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private Material getJobMaterial(int level) {
        return switch (jobType) {
            case MINER -> level >= 50 ? Material.DIAMOND_PICKAXE : Material.IRON_PICKAXE;
            case FARMER -> level >= 50 ? Material.NETHERITE_HOE : Material.GOLDEN_HOE;
            case FISHER -> Material.FISHING_ROD;
            case HUNTER -> Material.CROSSBOW;
            case ADVENTURER -> Material.FILLED_MAP;
        };
    }

    private Material getMaterialForStat1() {
        return switch (jobType) {
            case MINER -> Material.COAL;
            case FARMER -> Material.WHEAT;
            case FISHER -> Material.COD;
            case HUNTER -> Material.BONE;
            case ADVENTURER -> Material.COMPASS;
        };
    }

    private Material getMaterialForStat2() {
        return switch (jobType) {
            case MINER -> Material.DIAMOND;
            case FARMER -> Material.GOLDEN_CARROT;
            case FISHER -> Material.TROPICAL_FISH;
            case HUNTER -> Material.ENDER_PEARL;
            case ADVENTURER -> Material.MAP;
        };
    }

    private String getJobColor() {
        return switch (jobType) {
            case MINER -> "§6";
            case FARMER -> "§a";
            case FISHER -> "§b";
            case HUNTER -> "§c";
            case ADVENTURER -> "§d";
        };
    }

    private String getJobName() {
        return plugin.getConfigManager().getMessage("job-names." + jobType.name());
    }

    private String createProgressBar(double percent) {
        int total = 20;
        int filled = (int) ((percent / 100) * total);

        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < filled && i < total; i++)
            bar.append("▮");
        bar.append("§7");
        for (int i = filled; i < total; i++)
            bar.append("▯");

        return bar.toString();
    }
}
