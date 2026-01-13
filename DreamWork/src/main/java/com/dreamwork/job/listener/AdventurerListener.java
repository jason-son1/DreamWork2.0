package com.dreamwork.job.listener;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import com.dreamwork.mission.MissionType;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * 모험가 리스너
 * 
 * 험지 주파(이동 속도), 낙하 피해 감소, 귀환석, 좌표 스크롤, 탐험 보상 처리.
 */
public class AdventurerListener implements Listener {

    private final DreamWorkPlugin plugin;
    private final NamespacedKey recallKey;
    private final NamespacedKey scrollKeyX;
    private final NamespacedKey scrollKeyZ;
    private final NamespacedKey scrollKeyWorld;

    // 귀환 캐스팅 중인 플레이어 (이동 체크용)
    private final Map<UUID, RecallTask> recallTasks = new HashMap<>();

    public AdventurerListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        this.recallKey = new NamespacedKey(plugin, "recall_stone");
        this.scrollKeyX = new NamespacedKey(plugin, "scroll_x");
        this.scrollKeyZ = new NamespacedKey(plugin, "scroll_z");
        this.scrollKeyWorld = new NamespacedKey(plugin, "scroll_world");
    }

    /**
     * 험지 주파 (Pathfinder) - 이동 속도
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return; // Only execute on block change
        }

        // 1. Recall Cancel Check
        if (recallTasks.containsKey(player.getUniqueId())) {
            RecallTask task = recallTasks.get(player.getUniqueId());
            if (task.startLoc.distanceSquared(event.getTo()) > 0.5) {
                task.cancel();
                recallTasks.remove(player.getUniqueId());
                player.sendMessage("§c움직여서 귀환이 취소되었습니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            }
        }

        // 2. Pathfinder Speed
        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.ADVENTURER);
        if (level < 10)
            return;

        if (player.isFlying())
            return;

        Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        applySpeedBoost(player, block.getType(), level);

        // 3. Biome Discovery (Simple check)
        handlediscovery(player, level);
    }

    private void applySpeedBoost(Player player, Material ground, int level) {
        // Should reset speed if not on path?
        // This is complex because other plugins might set speed.
        // Let's set speed only if matching, else set default (0.2).
        // Careful not to override too much.

        float baseSpeed = 0.2f;
        float newSpeed = baseSpeed;

        boolean boost = false;

        // Logic based on Level
        if (level >= 50) {
            newSpeed = 0.24f; // +20% global
            boost = true;
        } else {
            if (level >= 30
                    && (ground == Material.SAND || ground == Material.GRAVEL || ground == Material.SNOW_BLOCK)) {
                newSpeed = 0.23f; // +15%
                boost = true;
            } else if (level >= 10 && (ground == Material.DIRT_PATH
                    || ground == Material.SHORT_GRASS)) { // DIRT_PATH check
                newSpeed = 0.22f; // +10%
                boost = true;
            }
        }

        if (player.getWalkSpeed() != newSpeed) {
            player.setWalkSpeed(newSpeed);
        }
    }

    private void handlediscovery(Player player, int level) {
        // 1. 바이옴 발견
        Biome biome = player.getLocation().getBlock().getBiome();
        plugin.getMissionManager().processEvent(player, MissionType.VISIT_BIOME, biome.name(), 1);

        // 2. 구조물 발견은 SixthSense 패시브 스킬로 이관됨 (최적화)
        // handlediscovery에서는 바이옴만 체크함.
    }

    /**
     * 낙하 피해 감소 (Pathfinder Lv 50)
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL)
            return;
        if (!(event.getEntity() instanceof Player player))
            return;

        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.ADVENTURER);
        if (level < 50)
            return;

        // Sneak Roll
        if (player.isSneaking()) {
            event.setCancelled(true);
            player.sendMessage("§b구르기!"); // Roll
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BIG_FALL, 1.0f, 0.5f);
            // Cooldown logic should be here
        } else {
            // Reduction
            event.setDamage(event.getDamage() * 0.8);
        }
    }

    /**
     * 아이템 사용 (귀환석, 좌표 스크롤)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null)
            return;
        Player player = event.getPlayer();

        // Recall Stone
        if (item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("귀환석")) {
            useRecallStone(player, item);
            event.setCancelled(true);
            return;
        }

        // Biome Capsule Logic
        String itemId = plugin.getItemManager().getDreamItemId(item);
        if ("biome_capsule".equals(itemId)) {
            // Empty Capsule -> Fill
            fillBiomeCapsule(player, item);
            event.setCancelled(true);
            return;
        }

        // Coordinate Scroll Usage
        if (item.getType() == Material.PAPER && item.hasItemMeta()) {
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (pdc.has(scrollKeyX, PersistentDataType.INTEGER)) {
                int x = pdc.get(scrollKeyX, PersistentDataType.INTEGER);
                int z = pdc.get(scrollKeyZ, PersistentDataType.INTEGER);
                String worldName = pdc.get(scrollKeyWorld, PersistentDataType.STRING);

                if (player.getWorld().getName().equals(worldName)) {
                    player.setCompassTarget(new Location(player.getWorld(), x, 64, z));
                    player.sendMessage("§e나침반이 좌표 (" + x + ", " + z + ")를 가리킵니다.");
                    event.setCancelled(true);
                }
            } else if (player.isSneaking()) {
                // Create Scroll (Command substitute: Shift+RightClick with empty paper)
                createScroll(player, item);
                event.setCancelled(true);
            }
        }
    }

    private void fillBiomeCapsule(Player player, ItemStack item) {
        // 쿨다운 등 체크
        Biome biome = player.getLocation().getBlock().getBiome();
        String biomeName = biome.name();

        // 캡슐 채우기 (아이템 소모 + 새로운 아이템 지급 or 메타 변경)
        // 메타 변경이 간단함
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b바이옴 캡슐: §f" + biomeName);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, "stored_biome"), PersistentDataType.STRING, biomeName);

        // ID 변경 (filled_biome_capsule) - ItemManager를 통해 템플릿 로드하면 좋지만,
        // 여기선 메타만 변경하여 "충전된" 상태로 만듦.
        // 혹은 아이템 교체
        item.setItemMeta(meta);

        player.sendMessage("§a현재 바이옴(" + biomeName + ")을 캡슐에 담았습니다.");
        player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1.0f, 1.0f);

        // 미션 진행
        plugin.getMissionManager().processEvent(player, MissionType.VISIT_BIOME, biomeName, 1);
    }

    private void useRecallStone(Player player, ItemStack item) {
        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.ADVENTURER);
        if (level < 10) {
            player.sendMessage("§c레벨 10부터 사용할 수 있습니다.");
            return;
        }

        if (recallTasks.containsKey(player.getUniqueId()))
            return; // Already casting

        // Cooldown check (Simple)

        int castTime = 10;
        if (level >= 50)
            castTime = 5;
        else if (level >= 30)
            castTime = 7;

        player.sendMessage("§b귀환 캐스팅 시작... (" + castTime + "초)");

        RecallTask task = new RecallTask(player, castTime);
        task.runTaskTimer(plugin, 0L, 20L);
        recallTasks.put(player.getUniqueId(), task);
    }

    private void createScroll(Player player, ItemStack item) {
        // Must be simple paper
        if (item.getAmount() > 1) {
            player.sendMessage("§c종이를 한 장만 들고 시도해주세요.");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        Location loc = player.getLocation();

        meta.setDisplayName("§e[좌표 스크롤] §f" + loc.getBlockX() + ", " + loc.getBlockZ());
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(scrollKeyX, PersistentDataType.INTEGER, loc.getBlockX());
        pdc.set(scrollKeyZ, PersistentDataType.INTEGER, loc.getBlockZ());
        pdc.set(scrollKeyWorld, PersistentDataType.STRING, loc.getWorld().getName());

        item.setItemMeta(meta);
        player.sendMessage("§a현재 위치가 기록되었습니다.");
    }

    private class RecallTask extends BukkitRunnable {
        Player player;
        Location startLoc;
        int timeLeft;

        RecallTask(Player player, int time) {
            this.player = player;
            this.startLoc = player.getLocation();
            this.timeLeft = time;
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                cancel();
                recallTasks.remove(player.getUniqueId());
                return;
            }

            if (timeLeft <= 0) {
                // TP
                Location spawn = player.getBedSpawnLocation();
                if (spawn == null)
                    spawn = player.getWorld().getSpawnLocation();

                player.teleport(spawn);
                player.sendMessage("§b귀환했습니다.");
                player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 1.0f);

                cancel();
                recallTasks.remove(player.getUniqueId());
            } else {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent("§b귀환 중... " + timeLeft + "초"));
                timeLeft--;
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        recallTasks.remove(event.getPlayer().getUniqueId());
    }
}
