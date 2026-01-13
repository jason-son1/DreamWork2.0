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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 미션 게시판 GUI
 * 
 * 현재 진행 중인 미션과 완료된 미션을 확인하고 보상을 수령합니다.
 * 
 * @author DreamWork Team
 */
public class MissionBoardGui extends DreamGui {

    private final DreamWorkPlugin plugin;

    // 슬롯 -> 미션 ID 매핑
    private final java.util.Map<Integer, String> slotMap = new java.util.HashMap<>();

    public MissionBoardGui(DreamWorkPlugin plugin, Player player) {
        super(plugin, player, "§d◆ 미션 게시판 📜 임무 목록 ◆", 6);
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        slotMap.clear();

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
        Map<String, PlayerMissionData> allMissions = userData.getAllMissions();

        // 진행 중이거나 보상 수령 대기 중인 미션만 필터링 및 정렬
        List<PlayerMissionData> displayList = allMissions.values().stream()
                .filter(m -> m.getStatus() != MissionStatus.CLAIMED) // 완료된(보상받은) 것은 숨김
                .filter(m -> m.getStatus() != MissionStatus.NOT_STARTED)
                .sorted(Comparator.comparing((PlayerMissionData m) -> m.getStatus() == MissionStatus.COMPLETED ? 0 : 1) // 완료된
                                                                                                                        // 것
                                                                                                                        // 우선
                        .thenComparing(PlayerMissionData::getStartTime)) // 오래된 순
                .collect(Collectors.toList());

        int slot = 10;
        for (PlayerMissionData data : displayList) {
            // 슬롯 범위를 벗어나면 중단 (페이지 기능은 추후 구현)
            if (slot > 43)
                break;
            if (slot % 9 == 8)
                slot += 2; // 오른쪽 가장자리 비우기

            String missionId = data.getMissionId();
            MissionTemplate template = plugin.getMissionManager().getMission(missionId);

            if (template == null)
                continue;

            inventory.setItem(slot, createMissionItem(data, template));
            slotMap.put(slot, missionId);
            slot++;
        }

        // 빈 슬롯에 안내 메시지
        if (displayList.isEmpty()) {
            inventory.setItem(22, createItem(Material.BOOK, "§7진행 중인 미션이 없습니다.",
                    List.of("§7마을의 NPC를 만나거나", "§7새로운 활동을 시작해보세요!")));
        }
    }

    private ItemStack createMissionItem(PlayerMissionData data, MissionTemplate template) {
        MissionStatus status = data.getStatus();

        // 아이콘 설정 (템플릿 아이콘 또는 기본값)
        Material material = template.getIcon();
        if (material == null) {
            material = (status == MissionStatus.COMPLETED) ? Material.EMERALD_BLOCK : Material.WRITABLE_BOOK;
        }

        String titlePrefix;
        List<String> lore = new ArrayList<>();

        if (template.getDescription() != null) {
            lore.addAll(template.getDescription());
            lore.add("§f▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        }

        // 상태 표시
        if (status == MissionStatus.COMPLETED) {
            titlePrefix = "§a✅ [완료] ";
            lore.add("§a클릭하여 보상 수령!");
        } else if (status == MissionStatus.IN_PROGRESS) {
            titlePrefix = "§e⏳ [진행 중] ";

            // 진행도 표시
            int progress = data.getProgress();
            int max = template.getAmount();
            int percent = max > 0 ? (int) ((double) progress / max * 100) : 0;

            lore.add("§f진행도: §e" + progress + " §7/ §6" + max + " §7(" + percent + "%)");
            lore.add(createProgressBar(percent));
        } else {
            titlePrefix = "§f[미션] ";
        }

        lore.add("§7");
        lore.add("§e[목표]");
        // 심플한 목표 표시 (템플릿의 targets 활용)
        if (template.getTargets() != null && !template.getTargets().isEmpty()) {
            // 첫 번째 타겟만 대표로 표시하거나 "..." 처리
            String targetName = template.getTargets().get(0);
            lore.add("§f- " + targetName + ": " + template.getAmount() + "회");
        } else {
            lore.add("§f- " + template.getType().name());
        }

        lore.add("§7");
        lore.add("§e[보상]");
        if (template.getRewardMoney() > 0) {
            lore.add("§f- " + template.getRewardMoney() + "G");
        }
        if (template.getRewardJobExp() != null && !template.getRewardJobExp().isEmpty()) {
            template.getRewardJobExp().forEach((job, exp) -> lore.add("§f- " + job + ": " + exp + " EXP"));
        }
        if (!template.getRewardItems().isEmpty()) {
            lore.add("§f- 아이템 " + template.getRewardItems().size() + "종");
        }
        if (template.getRewardBuffs() != null && !template.getRewardBuffs().isEmpty()) {
            lore.add("§f- 특수 버프");
        }

        // 아이템 생성
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(titlePrefix + template.getDisplayName());
            meta.setLore(lore);

            // 완료 상태면 글로우 효과
            if (status == MissionStatus.COMPLETED) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
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

        // 슬롯 매핑 확인
        String missionId = slotMap.get(slot);
        if (missionId != null) {
            PlayerMissionData data = plugin.getUserDataManager().getUserData(player).getMission(missionId);
            if (data != null && data.getStatus() == MissionStatus.COMPLETED) {
                // 보상 수령
                plugin.getMissionManager().completeMission(player, missionId);
                // GUI 갱신
                refresh();
            } else {
                // 진행 중인 미션 클릭 시 (상세 정보 띄우기 등 가능하나 일단 소리만)
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }
    }
}
