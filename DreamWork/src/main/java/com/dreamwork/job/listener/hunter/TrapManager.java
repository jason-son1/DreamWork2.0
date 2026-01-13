package com.dreamwork.job.listener.hunter;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

/**
 * 사냥용 덫 (Bear Trap) 매니저
 * 
 * 마커 엔티티를 사용하여 덫의 위치와 상태를 관리합니다.
 */
public class TrapManager {

    private final DreamWorkPlugin plugin;
    private final NamespacedKey trapKey;

    public TrapManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        this.trapKey = new NamespacedKey(plugin, "bear_trap");

        // 덫 감지 태스크 시작 (1초마다)
        startTrapTask();
    }

    /**
     * 덫 설치
     */
    public void placeTrap(Location loc, Player owner) {
        loc.getWorld().spawn(loc.clone().add(0, 0.1, 0), Marker.class, marker -> {
            marker.getPersistentDataContainer().set(trapKey, PersistentDataType.STRING, owner.getUniqueId().toString());
            marker.setCustomName("§c[덫]");

            // 시각적 피드백
            loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.5f, 2.0f);
            loc.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, loc.clone().add(0.5, 0.1, 0.5), 5, 0.1, 0, 0.1,
                    0.05);
        });

        owner.sendMessage("§c[사냥꾼] §f덫을 설치했습니다.");
    }

    /**
     * 덫 감지 태스크
     */
    private void startTrapTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                    Collection<Marker> markers = world.getEntitiesByClass(Marker.class);
                    for (Marker marker : markers) {
                        if (marker.getPersistentDataContainer().has(trapKey, PersistentDataType.STRING)) {
                            checkTrapTrigger(marker);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // 1초마다 실행
    }

    private void checkTrapTrigger(Marker trap) {
        // 주변 몬스터 감지 (0.5m 내)
        Collection<Entity> nearby = trap.getWorld().getNearbyEntities(trap.getLocation(), 0.7, 0.5, 0.7);
        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity victim && !(entity instanceof Player)) {
                triggerTrap(trap, victim);
                break;
            }
        }
    }

    private void triggerTrap(Marker trap, LivingEntity victim) {
        Location loc = trap.getLocation();

        // 효과 적용
        victim.damage(10.0); // 고정 피해
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 255)); // 5초간 이동 불가

        // 파티클 및 사운드
        loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.5f);
        loc.getWorld().spawnParticle(org.bukkit.Particle.CRIT, loc.add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.2);

        // 덫 제거
        trap.remove();

        plugin.debug("Trap triggered on " + victim.getType().name() + " at " + loc.toVector());
    }
}
