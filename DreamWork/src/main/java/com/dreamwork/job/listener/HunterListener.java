package com.dreamwork.job.listener;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import com.dreamwork.mission.MissionType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * 사냥꾼 직업 리스너
 * 
 * EntityDeathEvent를 감지하여 몬스터 처치 시 경험치와 돈을 지급합니다.
 * 스포너에서 생성된 몹은 보상이 감소합니다.
 * 
 * @author DreamWork Team
 */
public class HunterListener implements Listener {

    private final DreamWorkPlugin plugin;
    private static final String SPAWNER_METADATA = "dw_from_spawner";

    public HunterListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 엔티티 스폰 이벤트 - 스포너 태그 부착
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // 스포너에서 생성된 몹에 메타데이터 표시
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            event.getEntity().setMetadata(SPAWNER_METADATA,
                    new FixedMetadataValue(plugin, true));
        }
    }

    /**
     * 엔티티 사망 이벤트 - 몬스터 처치 처리
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        // 플레이어가 처치한 경우만
        if (killer == null)
            return;

        // 몬스터만 처리
        if (!isHostileMob(entity.getType()))
            return;

        // 직업 설정 가져오기
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig("hunter");
        if (jobConfig == null)
            return;

        // 몹별 보상 확인
        ConfigurationSection mobsSection = jobConfig.getConfigurationSection("mobs");
        if (mobsSection == null)
            return;

        String mobKey = entity.getType().name().toLowerCase();
        ConfigurationSection mobConfig = mobsSection.getConfigurationSection(mobKey);

        // 설정에 없으면 기본값 사용
        double exp = 10.0;
        double money = 5.0;

        if (mobConfig != null) {
            exp = mobConfig.getDouble("exp", 10.0);
            money = mobConfig.getDouble("money", 5.0);
        }

        // 스포너 몹은 보상 감소
        boolean fromSpawner = entity.hasMetadata(SPAWNER_METADATA);
        if (fromSpawner) {
            double spawnerMultiplier = jobConfig.getDouble("spawner_multiplier", 0.2);
            exp *= spawnerMultiplier;
            money *= spawnerMultiplier;
        }

        // 쿨다운 체크
        var userData = plugin.getUserDataManager().getUserData(killer);
        long cooldown = plugin.getConfigManager().getConfig()
                .getLong("anti-abuse.action-cooldown", 500);

        if (!userData.checkAndUpdateCooldown("kill_" + mobKey, cooldown)) {
            return;
        }

        // 보상 지급
        plugin.getJobManager().giveReward(killer, JobType.HUNTER, exp, money);

        // 미션 이벤트 트리거
        plugin.getMissionManager().processEvent(killer, MissionType.KILL, mobKey, 1);

        plugin.debug(killer.getName() + " 처치: " + mobKey +
                (fromSpawner ? " [스포너]" : "") +
                " (Exp: " + exp + ", Money: " + money + ")");
    }

    /**
     * 적대적 몹인지 확인
     */
    private boolean isHostileMob(EntityType type) {
        return switch (type) {
            case ZOMBIE, SKELETON, SPIDER, CAVE_SPIDER, CREEPER,
                    ENDERMAN, WITCH, SLIME, MAGMA_CUBE, BLAZE,
                    GHAST, WITHER_SKELETON, PIGLIN, PIGLIN_BRUTE,
                    HOGLIN, ZOGLIN, ZOMBIFIED_PIGLIN, DROWNED,
                    HUSK, STRAY, PHANTOM, GUARDIAN, ELDER_GUARDIAN,
                    SHULKER, VEX, EVOKER, VINDICATOR, PILLAGER,
                    RAVAGER, WARDEN, WITHER, ENDER_DRAGON ->
                true;
            default -> false;
        };
    }
}
