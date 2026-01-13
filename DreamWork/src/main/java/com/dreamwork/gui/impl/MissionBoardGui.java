package com.dreamwork.gui.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.gui.DreamGui;
import com.dreamwork.mission.MissionStatus;
import com.dreamwork.mission.PlayerMissionData;
import com.dreamwork.mission.MissionTemplate;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 미션 게시판 GUI
 * 
 * 현재 진행 중인 미션과 완료된 미션을 확인하고 보상을 수령합니다.
 * 
 * @author DreamWork Team
 */
public class MissionBoardGui extends DreamGui {

    private final DreamWorkPlugin plugin;

    public MissionBoardGui(DreamWorkPlugin plugin, Player player) {
        super(plugin, player, "§d◆ 미션 게시판 📜 임무 목록 ◆", 6);
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        // 배경 채우기
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        // 미션 목록 표시
        displayMissions();

        // 뒤로 가기
        inventory.setItem(49, createItem(Material.ARROW, "§c[ ← 뒤로 가기 ]", List.of()));
    }

    private void displayMissions() {
        UserData userData = plugin.getUserDataManager().getUserData(player);
        Map<String, PlayerMissionData> missions = userData.getAllMissions();

        int slot = 10;
        for (PlayerMissionData data : missions.values()) {
            // 슬롯 범위를 벗어나면 중단 (페이지 기능은 추후 구현)
            if (slot > 43)
                break;
            if (slot % 9 == 8)
                slot += 2; // 오른쪽 가장자리 비우기

            String missionId = data.getMissionId();
            MissionTemplate template = plugin.getMissionManager().getMission(missionId);

            if (template == null)
                continue;

            inventory.setItem(slot++, createMissionItem(data, template));
        }
    }

    private ItemStack createMissionItem(PlayerMissionData data, MissionTemplate template) {
        MissionStatus status = data.getStatus();
        Material material;
        String titlePrefix;
        List<String> lore = new ArrayList<>();

        // 상태에 따른 아이콘 및 설명 설정
        if (status == MissionStatus.COMPLETED) {
            material = Material.EMERALD_BLOCK;
            titlePrefix = "§a✅ [완료] ";
            lore.add("§a클릭하여 보상 수령!");
        } else if (status == MissionStatus.IN_PROGRESS) {
            material = Material.WRITABLE_BOOK;
            titlePrefix = "§e⏳ [진행 중] ";

            // 진행도 표시
            int progress = data.getProgress();
            int max = template.getAmount();
            int percent = (int) ((double) progress / max * 100);

            lore.add("§7진행도: §f" + progress + " / " + max + " §7(" + percent + "%)");
            lore.add(createProgressBar(percent));
        } else if (status == MissionStatus.CLAIMED) {
            material = Material.BOOK;
            titlePrefix = "§7✔ [완료됨] ";
            lore.add("§7이미 보상을 수령했습니다.");
        } else {
            material = Material.PAPER;
            titlePrefix = "§f[미션] ";
        }

        lore.add("§7");
        lore.add("§e[목표]");
        for (String target : template.getTargets()) {
            lore.add("§f- " + target + " " + template.getAmount() + "회");
        }

        lore.add("§7");
        lore.add("§e[보상]");
        if (template.getRewardMoney() > 0) {
            lore.add("§f- " + template.getRewardMoney() + "G");
        }
        if (template.getRewardJobExp() != null && !template.getRewardJobExp().isEmpty()) {
            lore.add("§f- 직업 경험치");
        }
        if (!template.getRewardItems().isEmpty()) {
            lore.add("§f- 아이템 " + template.getRewardItems().size() + "종");
        }

        // 아이템 생성
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(titlePrefix + template.getDisplayName());
        meta.setLore(lore);

        // 완료 상태면 글로우 효과
        if (status == MissionStatus.COMPLETED) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private String createProgressBar(int percent) {
        int bars = 20;
        int filled = (int) ((percent / 100.0) * bars);
        StringBuilder sb = new StringBuilder("§8[§a");
        for (int i = 0; i < filled; i++)
            sb.append("|");
        sb.append("§7");
        for (int i = filled; i < bars; i++)
            sb.append("|");
        sb.append("§8]");
        return sb.toString();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == 49) { // 뒤로 가기
            plugin.getGuiManager().openMainDashboard(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // 미션 아이템 클릭
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        // 아이템에서 미션 정보를 역추적하기 어려우므로 슬롯 인덱스로 매핑하거나 NBT를 써야하는데,
        // 여기서는 다시 로직을 돌려서 찾기보다는 간단하게 클릭 시 동작을 정의합니다.
        // 현재 구조상 슬롯과 미션 매핑을 저장해두는 것이 좋습니다.
        // 하지만 간단하게 구현하기 위해, UserData를 순회하며 해당 슬롯에 맞는 미션을 찾습니다.

        UserData userData = plugin.getUserDataManager().getUserData(player);
        Map<String, PlayerMissionData> missions = userData.getAllMissions();

        int currentSlot = 10;
        String clickedMissionId = null;

        for (PlayerMissionData data : missions.values()) {
            if (currentSlot > 43)
                break;
            if (currentSlot % 9 == 8)
                currentSlot += 2;

            if (currentSlot == slot) {
                clickedMissionId = data.getMissionId();
                break;
            }
            currentSlot++;
        }

        if (clickedMissionId != null) {
            PlayerMissionData data = userData.getMission(clickedMissionId);
            if (data.getStatus() == MissionStatus.COMPLETED) {
                // 보상 수령
                plugin.getMissionManager().completeMission(player, clickedMissionId);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                // GUI 갱신
                refresh();
            } else {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }
    }
}
