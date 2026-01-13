package com.dreamwork.job.listener;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import com.dreamwork.mission.MissionType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

/**
 * 사냥꾼 직업 리스너
 * 
 * 엘리트 몹 스폰, 보상 지급, 스킬(약점 간파, 지식, 추적) 구현.
 */
public class HunterListener implements Listener {

    private final DreamWorkPlugin plugin;
    private final Random random = new Random();
    private final NamespacedKey eliteKey;
    private final NamespacedKey killCountKey; // Prefix for player PDC

    public HunterListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        this.eliteKey = new NamespacedKey(plugin, "elite_mob");
        this.killCountKey = new NamespacedKey(plugin, "mob_kill_");
    }

    /**
     * 엘리트 몹 스폰 (1%)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL)
            return;

        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster))
            return;

        FileConfiguration config = plugin.getConfigManager().getJobConfig("hunter");
        double chance = config.getDouble("items.elite_spawn_chance", 0.01);

        if (random.nextDouble() < chance) {
            makeElite(entity);
        }
    }

    private void makeElite(LivingEntity entity) {
        entity.getPersistentDataContainer().set(eliteKey, PersistentDataType.BYTE, (byte) 1);

        // Stat Boost
        double maxHealth = 20.0;
        if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
            maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getBaseValue() * 2.0;
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
        }
        entity.setHealth(maxHealth);

        // Visuals
        entity.setCustomName("§c[Elite] §f" + entity.getName());
        entity.setCustomNameVisible(true);
        entity.setGlowing(true); // Optional

        // Equipment (Simple)
        if (entity.getEquipment() != null) {
            entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            entity.getEquipment().setItemInMainHandDropChance(0.0f);
        }
    }

    /**
     * 전투 스킬 (약점 간파, 지식 데미지 보너스)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 1. Attacker is Hunter
        if (event.getDamager() instanceof Player attacker) {
            UserData userData = plugin.getUserDataManager().getUserData(attacker);
            int level = userData.getJobLevel(JobType.HUNTER);

            if (level > 0 && event.getEntity() instanceof LivingEntity victim) {
                applyHunterDamageBonus(attacker, level, victim, event);
            }
        }

        // 2. Victim is Hunter (Knowledge Defense Bonus)
        if (event.getEntity() instanceof Player victim) {
            UserData userData = plugin.getUserDataManager().getUserData(victim);
            int level = userData.getJobLevel(JobType.HUNTER);
            if (level > 0 && event.getDamager() instanceof LivingEntity attacker) {
                applyHunterDefenseBonus(victim, attacker, event);
            }
        }
    }

    private void applyHunterDamageBonus(Player player, int level, LivingEntity victim,
            EntityDamageByEntityEvent event) {
        FileConfiguration config = plugin.getConfigManager().getJobConfig("hunter");

        // Vital Strike (Crit)
        double chance = 0;
        double mult = 1.0;
        if (level >= 50) {
            chance = 0.15;
            mult = 2.0;
        } else if (level >= 30) {
            chance = 0.10;
            mult = 1.5;
        } else if (level >= 10) {
            chance = 0.05;
            mult = 1.2;
        }

        if (random.nextDouble() < chance) {
            event.setDamage(event.getDamage() * mult);
            player.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
            player.spawnParticle(org.bukkit.Particle.CRIT, victim.getLocation().add(0, 1, 0), 15, 0.2, 0.2, 0.2, 0.1);

            if (level >= 50) { // Speed Buff
                player.addPotionEffect(
                        new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 20, 1));
            }
            if (level >= 30 && event.getDamager() instanceof Arrow) { // Knockback on Bow?
                // Hard to apply interactively on Arrow hit, but we can push victim
                victim.setVelocity(victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize()
                        .multiply(1.5));
            }
        }

        // 지식 보너스 (티어 1: 데미지 5% 증가)
        int kills = getKillCount(player, victim.getType());
        if (kills >= 100) {
            event.setDamage(event.getDamage() * 1.05);
        }

        // 컨텍스트 마킹 (헤드샷, 거리)
        boolean isHeadshot = false;
        if (event.getDamager() instanceof Arrow arrow) {
            // 간단한 헤드샷 체크: 화살 높이 vs 눈 높이
            double arrowY = arrow.getLocation().getY();
            double eyeY = victim.getEyeLocation().getY();
            if (Math.abs(arrowY - eyeY) < 0.5) {
                isHeadshot = true;
            }
        }
        double distance = player.getLocation().distance(victim.getLocation());
        markCombatContext(victim, player, isHeadshot, distance);
    }

    private void applyHunterDefenseBonus(Player player, LivingEntity attacker, EntityDamageByEntityEvent event) {
        // Knowledge Bonus (Tier 2: -10% Dmg)
        int kills = getKillCount(player, attacker.getType());
        if (kills >= 500) {
            event.setDamage(event.getDamage() * 0.90);
        }
    }

    /**
     * 몬스터 처치 보상
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null)
            return;

        // Kill Count Update
        incrementKillCount(killer, entity.getType());

        // Rewards logic
        String mobKey = entity.getType().name().toLowerCase();
        boolean isElite = entity.getPersistentDataContainer().has(eliteKey, PersistentDataType.BYTE);

        if (isElite)
            mobKey = "elite_mob";

        FileConfiguration config = plugin.getConfigManager().getJobConfig("hunter");
        double exp = 0;
        double money = 0;

        if (config.contains("rewards." + mobKey)) {
            exp = config.getDouble("rewards." + mobKey + ".exp");
            money = config.getDouble("rewards." + mobKey + ".money");
        }

        // Knowledge Bonus (Tier 3: 20% Boost)
        int kills = getKillCount(killer, entity.getType());
        if (kills >= 1000) {
            exp *= 1.2;
            money *= 1.2;
        }

        if (exp > 0 || money > 0) {
            plugin.getJobManager().giveReward(killer, JobType.HUNTER, exp, money);
            plugin.getMissionManager().processEvent(killer, MissionType.KILL, entity.getType().name(), 1);

            // 조건부 처치 컨텍스트 (헤드샷, 거리 등)
            java.util.Map<String, Object> context = new java.util.HashMap<>();
            if (entity.hasMetadata("dreamwork:headshot")) {
                context.put("isHeadshot", entity.getMetadata("dreamwork:headshot").get(0).value());
            }
            if (entity.hasMetadata("dreamwork:distance")) {
                context.put("distance", entity.getMetadata("dreamwork:distance").get(0).value());
            }
            if (entity instanceof org.bukkit.entity.Ageable ageable && !ageable.isAdult()) {
                context.put("isBaby", true);
            }
            if (isElite) {
                context.put("isElite", true); // 선택적 컨텍스트
            }

            // 조건부 미션 트리거
            plugin.getMissionManager().processEvent(killer, MissionType.CONDITIONAL_KILL, entity.getType().name(), 1,
                    context);
            // "ANY" 타겟 미션도 트리거 (필요 시)
            plugin.getMissionManager().processEvent(killer, MissionType.CONDITIONAL_KILL, "ANY", 1, context);

            if (isElite) {
                Bukkit.broadcastMessage("§c[사냥꾼] §f" + killer.getName() + "님이 §4엘리트 몬스터§f를 처치했습니다!");
            }
        }
    }

    private void markCombatContext(LivingEntity victim, Player attacker, boolean isHeadshot, double distance) {
        victim.setMetadata("dreamwork:headshot", new org.bukkit.metadata.FixedMetadataValue(plugin, isHeadshot));
        victim.setMetadata("dreamwork:distance", new org.bukkit.metadata.FixedMetadataValue(plugin, distance));
    }

    /**
     * 추적 스킬 (나침반 우클릭)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.COMPASS)
            return;
        if (!player.isSneaking())
            return;
        if (!event.getAction().name().contains("RIGHT"))
            return;

        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.HUNTER);
        if (level <= 0)
            return;

        // Range
        double range = 20;
        if (level >= 50)
            range = 100;
        else if (level >= 20)
            range = 50;

        // Scan for Elite or Hostile Player
        LivingEntity target = null;
        double minDst = Double.MAX_VALUE;

        List<Entity> nearby = player.getNearbyEntities(range, range, range);
        for (Entity e : nearby) {
            if (e instanceof LivingEntity le) {
                boolean isElite = le.getPersistentDataContainer().has(eliteKey, PersistentDataType.BYTE);
                // Also check hostile players (Simplification: just finding other players)
                boolean isPlayer = (e instanceof Player) && (e != player);

                if (isElite || isPlayer) {
                    double dst = player.getLocation().distanceSquared(e.getLocation());
                    if (dst < minDst) {
                        minDst = dst;
                        target = le;
                    }
                }
            }
        }

        if (target != null) {
            String name = target.getName();
            int dist = (int) Math.sqrt(minDst);
            if (level >= 20) {
                // Direction
                Vector dir = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                String direction = getCompassDirection(dir);
                player.sendMessage("§a[추적] §f" + direction + "쪽 " + dist + "m 거리에 §c" + name + "§f(이)가 있습니다.");

                if (level >= 50) {
                    target.setGlowing(true); // Temporary glow? Should reset later.
                    // Just leaving it glowing might be annoying.
                    final LivingEntity ft = target;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> ft.setGlowing(false), 60L); // 3s
                }
            } else {
                player.sendMessage("§a[추적] §f주변에서 강력한 기운이 느껴집니다...");
            }
        } else {
            player.sendMessage("§7[추적] 주변에 감지되는 대상이 없습니다.");
        }
    }

    private void incrementKillCount(Player player, EntityType type) {
        UserData data = plugin.getUserDataManager().getUserData(player);
        // We can store kills in UserData or PDC on Player.
        // For simplicity and persistence, let's assume UserData has a generic data map
        // or we use Player PDC.
        // Since UserData is our core manager, use it if possible.
        // But UserData interface might not have setInt(key, val).
        // Let's use getPersistentDataContainer on Player.

        NamespacedKey key = new NamespacedKey(plugin, "mob_kill_" + type.name());
        int current = 0;
        if (player.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
            current = player.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        }
        player.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, current + 1);
    }

    private int getKillCount(Player player, EntityType type) {
        NamespacedKey key = new NamespacedKey(plugin, "mob_kill_" + type.name());
        if (player.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
            return player.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        }
        return 0;
    }

    private String getCompassDirection(Vector dir) {
        double rot = Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        if (rot < 0)
            rot += 360;

        if (rot >= 315 || rot < 45)
            return "남";
        if (rot >= 45 && rot < 135)
            return "서";
        if (rot >= 135 && rot < 225)
            return "북";
        return "동"; // 225-315

        // Note: Minecraft coords: Z+ is South, X- is West?
        // atan2(-x, z) gives rotation.
        // Usually: S=0, W=90, N=180, E=270.
        // Adjust for desired output.
    }
}
