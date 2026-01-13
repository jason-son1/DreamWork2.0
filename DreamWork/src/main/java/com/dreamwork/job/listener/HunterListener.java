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
    private final com.dreamwork.job.listener.hunter.TrapManager trapManager;
    private final NamespacedKey eliteKey;
    private final NamespacedKey killCountKey;
    private final NamespacedKey noRewardKey;
    private final NamespacedKey headshotKey;
    private final NamespacedKey distanceKey;
    private final Random random = new Random();

    public HunterListener(DreamWorkPlugin plugin, com.dreamwork.job.listener.hunter.TrapManager trapManager) {
        this.plugin = plugin;
        this.trapManager = trapManager;
        this.eliteKey = new NamespacedKey(plugin, "elite_mob");
        this.killCountKey = new NamespacedKey(plugin, "mob_kill_");
        this.noRewardKey = new NamespacedKey(plugin, "no_reward");
        this.headshotKey = new NamespacedKey(plugin, "headshot");
        this.distanceKey = new NamespacedKey(plugin, "distance");
    }

    /**
     * 엘리트 몹 스폰 (1%)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();

        // 스포너 몹 보상 제외 태그
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            entity.getPersistentDataContainer().set(noRewardKey, PersistentDataType.BYTE, (byte) 1);
            return;
        }

        // 자연 스폰만 엘리트 몹 처리
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

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
        if (entity instanceof org.bukkit.entity.Player)
            return;

        // 보상 제외 체크 (스포너 등)
        if (entity.getPersistentDataContainer().has(new NamespacedKey(plugin, "no_reward"), PersistentDataType.BYTE)) {
            return;
        }

        Player killer = entity.getKiller();

        if (killer == null)
            return;

        // Towny 확인 (도시 내에서 보상 획득 제한)
        if (plugin.getTownyHook().isEnabled() && plugin.getTownyHook().getTownName(entity.getLocation()) != null) {
            // 도시 내부에서는 보상 없음 (어뷰징 방지)
            return;
        }

        // Kill Count Update
        incrementKillCount(killer, entity.getType());

        // Rewards logic
        String mobKey = entity.getType().name().toLowerCase();
        boolean isElite = entity.getPersistentDataContainer().has(eliteKey, PersistentDataType.BYTE);

        if (isElite) {
            mobKey = "elite_mob";

            // 엘리트 몹 드롭 처리
            if (event.getDrops().isEmpty()) {
                event.getDrops().add(new ItemStack(Material.GOLD_NUGGET));
            }

            // 첫 번째 드롭 아이템을 엘리트 전리품으로 변환
            if (!event.getDrops().isEmpty()) {
                ItemStack drop = event.getDrops().get(0);
                plugin.getItemManager().setEliteMobDrop(drop, entity.getType().name());
            }
        }

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

        // 현상수배 보너스
        if (plugin.getBountyManager().getCurrentTarget() == entity.getType()) {
            double multiplier = plugin.getBountyManager().getRewardMultiplier();
            exp *= multiplier;
            money *= multiplier;

            // 현상수배 완료 알림 (보상 획득 시)
            if (exp > 0 || money > 0) {
                killer.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.ComponentBuilder(
                                "§c[현상수배] §f목표 처치! 보상 §6x" + String.format("%.1f", multiplier)).create());
            }
        }

        if (exp > 0 || money > 0) {
            plugin.getJobManager().giveReward(killer, JobType.HUNTER, exp, money);
            plugin.getMissionManager().processEvent(killer, MissionType.KILL, entity.getType().name(), 1);

            // 조건부 처치 컨텍스트 (PDC 기반)
            java.util.Map<String, Object> context = new java.util.HashMap<>();
            if (entity.getPersistentDataContainer().has(headshotKey, PersistentDataType.BYTE)) {
                context.put("isHeadshot",
                        entity.getPersistentDataContainer().get(headshotKey, PersistentDataType.BYTE) == 1);
            }
            if (entity.getPersistentDataContainer().has(distanceKey, PersistentDataType.DOUBLE)) {
                context.put("distance",
                        entity.getPersistentDataContainer().get(distanceKey, PersistentDataType.DOUBLE));
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
        victim.getPersistentDataContainer().set(headshotKey, PersistentDataType.BYTE, (byte) (isHeadshot ? 1 : 0));
        victim.getPersistentDataContainer().set(distanceKey, PersistentDataType.DOUBLE, distance);
    }

    /**
     * 추적 스킬 (나침반 우클릭)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR)
            return;

        // 웅크리고 우클릭
        if (!player.isSneaking() || !event.getAction().name().contains("RIGHT"))
            return;

        String dwId = plugin.getItemManager().getDreamItemId(item);

        // 1. 사냥용 덫 (Bear Trap)
        if ("bear_trap".equals(dwId)) {
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                    && event.getClickedBlock() != null) {
                trapManager.placeTrap(event.getClickedBlock().getLocation().add(0, 1, 0), player);
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                }
                event.setCancelled(true);
            }
            return;
        }

        // 2. 제압용 목줄 (Taming Collar)
        if ("taming_collar".equals(dwId)) {
            // PlayerInteractEntityEvent 에서 처리하는게 나을 수 있으나 여기서 대상 확인
            // 여기서는 일단 취소하고 EntityInteract에서 처리하도록 유도하거나 직접 구현
            return;
        }

        if (item.getType() != Material.COMPASS)
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

    /**
     * 제압용 목줄 (Taming Collar) 사용
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR)
            return;

        String dwId = plugin.getItemManager().getDreamItemId(item);
        if (!"taming_collar".equals(dwId))
            return;

        event.setCancelled(true);

        if (!(entity instanceof LivingEntity victim) || (entity instanceof Player)) {
            player.sendMessage("§c해당 대상에게는 사용할 수 없습니다.");
            return;
        }

        // 제압 가능 체력 체크 (최대 체력의 20% 이하)
        double healthPercent = victim.getHealth() / victim.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (healthPercent > 0.30) { // 30% 이하로 완화
            player.sendMessage("§c대상이 너무 강렬하게 저항합니다! (체력을 더 낮추어야 합니다)");
            return;
        }

        // 확률 체크
        double successRate = 0.5; // 기본 50%
        if (random.nextDouble() < successRate) {
            player.sendMessage("§a[사냥꾼] §f성공적으로 §e" + victim.getName() + "§f을(를) 제압했습니다!");
            victim.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, victim.getLocation().add(0, 1, 0), 20,
                    0.5, 0.5, 0.5, 0.1);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 0.5f);

            // 몬스터 제거 및 전리품(보상) 지급
            // capture_mob 미션 트리거?
            plugin.getMissionManager().processEvent(player, MissionType.KILL, "CAPTURE_" + victim.getType().name(), 1);
            victim.remove();

            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
            }
        } else {
            player.sendMessage("§c제압에 실패했습니다! 대상이 날뜁니다.");
            victim.getWorld().spawnParticle(org.bukkit.Particle.ANGRY_VILLAGER, victim.getLocation().add(0, 1, 0), 5,
                    0.2, 0.2, 0.2, 0);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.5f);

            // 저항 (데미지)
            player.damage(2.0);
        }
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
