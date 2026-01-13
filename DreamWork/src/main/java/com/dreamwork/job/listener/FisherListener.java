package com.dreamwork.job.listener;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import com.dreamwork.mission.MissionType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * 어부 직업 리스너
 * 
 * PlayerFishEvent를 감지하여 낚시 시 경험치와 돈을 지급합니다.
 * 
 * @author DreamWork Team
 */
public class FisherListener implements Listener {

    private final DreamWorkPlugin plugin;
    private final Random random = new Random();

    public FisherListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 낚시 이벤트
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        // 낚시 성공 시에만 처리
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        Player player = event.getPlayer();
        Entity caught = event.getCaught();

        // 아이템을 낚았는지 확인
        if (!(caught instanceof Item caughtItem))
            return;

        ItemStack item = caughtItem.getItemStack();
        String itemType = item.getType().name().toLowerCase();

        // 직업 설정 가져오기
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig("fisher");
        if (jobConfig == null)
            return;

        // 물고기별 보상 확인
        ConfigurationSection fishSection = jobConfig.getConfigurationSection("fish");
        if (fishSection == null)
            return;

        ConfigurationSection fishConfig = fishSection.getConfigurationSection(itemType);

        // 설정에 없으면 기본값 사용
        double exp = 5.0;
        double money = 2.0;

        if (fishConfig != null) {
            exp = fishConfig.getDouble("exp", 5.0);
            money = fishConfig.getDouble("money", 2.0);
        }

        // 쿨다운 체크
        var userData = plugin.getUserDataManager().getUserData(player);
        long cooldown = plugin.getConfigManager().getConfig()
                .getLong("anti-abuse.action-cooldown", 500);

        if (!userData.checkAndUpdateCooldown("fish", cooldown)) {
            return;
        }

        // 보상 지급
        plugin.getJobManager().giveReward(player, JobType.FISHER, exp, money);

        // 미션 이벤트 트리거
        plugin.getMissionManager().processEvent(player, MissionType.FISH, itemType, 1);

        // 물고기 크기 생성 (cm)
        int size = generateFishSize(player);

        // 메시지 출력
        String fishName = getFishDisplayName(itemType);
        String message = plugin.getConfigManager()
                .getMessage("fisher.fish-caught")
                .replace("{fish}", fishName)
                .replace("{size}", String.valueOf(size));
        player.sendMessage(plugin.getConfigManager().getMessage("prefix") + message);

        plugin.debug(player.getName() + " 낚시: " + itemType + " " + size + "cm" +
                " (Exp: " + exp + ", Money: " + money + ")");
    }

    /**
     * 물고기 크기 생성 (가우시안 분포)
     */
    private int generateFishSize(Player player) {
        int baseSizeMin = 10;
        int baseSizeMax = 50;

        // 레벨 보너스 (레벨당 최대 크기 0.5cm 증가)
        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.FISHER);
        int levelBonus = (int) (level * 0.5);

        return baseSizeMin + random.nextInt(baseSizeMax - baseSizeMin + levelBonus);
    }

    /**
     * 물고기 표시 이름
     */
    private String getFishDisplayName(String itemType) {
        return switch (itemType) {
            case "cod" -> "대구";
            case "salmon" -> "연어";
            case "tropical_fish" -> "열대어";
            case "pufferfish" -> "복어";
            default -> itemType;
        };
    }
}
