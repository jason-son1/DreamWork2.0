package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 지하 적응 패시브 (광부)
 * 
 * Y좌표 일정 이하의 지하에서 활동 시 여러 버프를 제공합니다.
 * 레벨에 따라 효과가 강화됩니다.
 * 
 * 효과:
 * - 낙하 데미지 감소
 * - 채광 속도 증가 (성급함 효과)
 * - 용암 접촉 시 화염 저항 (쿨다운 있음)
 * 
 * @author DreamWork Team
 */
public class CaveAdaptation implements Listener {

    private final DreamWorkPlugin plugin;

    // 지하 적응 활성 상태 추적
    private final Map<UUID, Long> lastUndergroundCheck = new HashMap<>();

    // 화염 저항 쿨다운 (UUID -> 만료 시간)
    private final Map<UUID, Long> fireResistanceCooldown = new HashMap<>();

    public CaveAdaptation(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 플레이어 이동 시 지하 적응 버프 적용
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 성능 최적화: 1초에 한 번만 체크
        Long lastCheck = lastUndergroundCheck.get(uuid);
        long now = System.currentTimeMillis();
        if (lastCheck != null && now - lastCheck < 1000) {
            return;
        }
        lastUndergroundCheck.put(uuid, now);

        // 광부 레벨 확인
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int minerLevel = userData.getJobLevel(JobType.MINER);

        // 최소 레벨 확인
        int unlockLevel = plugin.getConfigManager().getJobConfig("miner")
                .getInt("skills.cave_adaptation.unlock-level", 10);

        if (minerLevel < unlockLevel) {
            return;
        }

        // Y좌표 확인
        int y = player.getLocation().getBlockY();
        int activationY = plugin.getConfigManager().getJobConfig("miner")
                .getInt("skills.cave_adaptation.activation-y", 0);

        boolean isUnderground = y <= activationY;
        boolean isMining = isMiningTool(player.getInventory().getItemInMainHand().getType());

        // 지하에서 곡괭이를 들고 있을 때만 버프 적용
        if (isUnderground && isMining) {
            applyUndergroundBuffs(player, minerLevel);
        }
    }

    /**
     * 낙하 데미지 감소
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        // 광부 레벨 확인
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int minerLevel = userData.getJobLevel(JobType.MINER);

        int unlockLevel = plugin.getConfigManager().getJobConfig("miner")
                .getInt("skills.cave_adaptation.unlock-level", 10);

        if (minerLevel < unlockLevel) {
            return;
        }

        // 지하에 있을 때만 적용
        int y = player.getLocation().getBlockY();
        int activationY = plugin.getConfigManager().getJobConfig("miner")
                .getInt("skills.cave_adaptation.activation-y", 0);

        if (y > activationY) {
            return;
        }

        // 레벨별 낙하 데미지 감소율
        double reduction = calculateFallDamageReduction(minerLevel);

        double originalDamage = event.getDamage();
        double reducedDamage = originalDamage * (1.0 - reduction);

        event.setDamage(reducedDamage);

        plugin.debug(player.getName() + " 지하 적응: 낙하 데미지 " +
                (int) (reduction * 100) + "% 감소 (" +
                String.format("%.1f", originalDamage) + " -> " +
                String.format("%.1f", reducedDamage) + ")");
    }

    /**
     * 용암 접촉 시 화염 저항 부여
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLavaDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.LAVA) {
            return;
        }

        // 광부 레벨 확인 (50 이상만)
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int minerLevel = userData.getJobLevel(JobType.MINER);

        if (minerLevel < 50) {
            return;
        }

        // 쿨다운 체크
        UUID uuid = player.getUniqueId();
        Long cooldownExpiry = fireResistanceCooldown.get(uuid);
        long now = System.currentTimeMillis();

        if (cooldownExpiry != null && now < cooldownExpiry) {
            return; // 쿨다운 중
        }

        // 화염 저항 부여 (5초)
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE,
                100, // 5초 (100틱)
                0,
                false,
                true,
                true));

        player.sendMessage("§c🔥 지하 적응: 용암 저항 발동!");

        // 쿨다운 시작 (60초)
        long cooldownDuration = plugin.getConfigManager().getJobConfig("miner")
                .getLong("skills.cave_adaptation.fire-resistance-cooldown", 60) * 1000;
        fireResistanceCooldown.put(uuid, now + cooldownDuration);

        plugin.debug(player.getName() + " 지하 적응: 화염 저항 발동 (60초 쿨다운)");
    }

    /**
     * 지하 버프 적용
     */
    private void applyUndergroundBuffs(Player player, int minerLevel) {
        // 성급함 효과 레벨 계산
        int hasteLevel = calculateHasteLevel(minerLevel);

        if (hasteLevel > 0) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE,
                    40, // 2초 (지속적으로 갱신됨)
                    hasteLevel - 1, // 0 = 레벨 1
                    false,
                    false,
                    false));
        }
    }

    /**
     * 낙하 데미지 감소율 계산
     */
    private double calculateFallDamageReduction(int level) {
        if (level < 10)
            return 0.0;
        if (level < 30)
            return 0.20; // 20% 감소
        if (level < 50)
            return 0.50; // 50% 감소
        return 0.80; // 80% 감소
    }

    /**
     * 성급함 효과 레벨 계산
     */
    private int calculateHasteLevel(int level) {
        if (level < 30)
            return 0;
        if (level < 50)
            return 1; // 성급함 I
        return 2; // 성급함 II
    }

    /**
     * 채굴 도구인지 확인
     */
    private boolean isMiningTool(Material material) {
        return switch (material) {
            case WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE,
                    GOLDEN_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE ->
                true;
            default -> false;
        };
    }
}
