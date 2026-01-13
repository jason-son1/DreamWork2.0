package com.dreamwork.skill.impl;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import org.bukkit.Chunk;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 육감 스킬 (탐험가)
 * 
 * 새로운 청크나 바이옴을 발견하면 알림과 보상을 제공합니다.
 * 패시브 스킬로, 해금 후 자동 적용됩니다.
 * 
 * 해금 레벨: 5
 * 
 * @author DreamWork Team
 */
public class ExplorerSense implements Listener {

    private final DreamWorkPlugin plugin;
    private static final String SKILL_ID = "explorer_sense";

    // 플레이어별 방문한 청크/바이옴 추적 (세션 동안만)
    private final Map<UUID, Set<String>> visitedChunks = new HashMap<>();
    private final Map<UUID, Set<Biome>> discoveredBiomes = new HashMap<>();

    public ExplorerSense(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 플레이어 이동 이벤트 - 청크/바이옴 발견 체크
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 청크가 변경되지 않았으면 무시
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 스킬 해금 확인
        UserData userData = plugin.getUserDataManager().getUserData(player);
        int level = userData.getJobLevel(JobType.ADVENTURER);
        int unlockLevel = plugin.getSkillManager().getSkillUnlockLevel(JobType.ADVENTURER, SKILL_ID);

        if (level < unlockLevel) {
            return;
        }

        Chunk chunk = event.getTo().getChunk();
        Biome biome = player.getLocation().getBlock().getBiome();

        // 방문한 청크 체크
        Set<String> playerChunks = visitedChunks.computeIfAbsent(uuid, k -> new HashSet<>());
        String chunkKey = chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();

        boolean newChunk = playerChunks.add(chunkKey);

        // 발견한 바이옴 체크
        Set<Biome> playerBiomes = discoveredBiomes.computeIfAbsent(uuid, k -> new HashSet<>());
        boolean newBiome = playerBiomes.add(biome);

        // 새로운 바이옴 발견 보상
        if (newBiome) {
            double biomeExp = plugin.getConfigManager().getJobConfig("adventurer")
                    .getDouble("skills.explorer_sense.biome_exp", 50.0);
            double biomeMoney = plugin.getConfigManager().getJobConfig("adventurer")
                    .getDouble("skills.explorer_sense.biome_money", 20.0);

            plugin.getJobManager().giveReward(player, JobType.ADVENTURER, biomeExp, biomeMoney);

            String biomeName = getBiomeDisplayName(biome);
            player.sendMessage("§e✦ 새로운 바이옴 발견! §f" + biomeName);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);

            plugin.debug(player.getName() + " 바이옴 발견: " + biome.getKey().getKey());
        }
        // 새로운 청크 (10개마다 보상)
        else if (newChunk && playerChunks.size() % 10 == 0) {
            double chunkExp = plugin.getConfigManager().getJobConfig("adventurer")
                    .getDouble("skills.explorer_sense.chunk_exp", 5.0);
            double chunkMoney = plugin.getConfigManager().getJobConfig("adventurer")
                    .getDouble("skills.explorer_sense.chunk_money", 2.0);

            plugin.getJobManager().giveReward(player, JobType.ADVENTURER, chunkExp * 10, chunkMoney * 10);

            player.sendMessage("§7탐험 보너스: " + playerChunks.size() + "개 청크 탐험 완료!");
        }
    }

    /**
     * 바이옴 표시 이름
     */
    private String getBiomeDisplayName(Biome biome) {
        String bioName = biome.getKey().getKey();
        return switch (bioName) {
            case "plains" -> "평원";
            case "forest" -> "숲";
            case "desert" -> "사막";
            case "jungle" -> "정글";
            case "taiga" -> "타이가";
            case "snowy_plains" -> "눈 평원";
            case "ocean" -> "바다";
            case "deep_ocean" -> "심해";
            case "river" -> "강";
            case "swamp" -> "늪";
            case "badlands" -> "악지";
            case "savanna" -> "사바나";
            case "beach" -> "해변";
            case "mushroom_fields" -> "버섯 지대";
            case "nether_wastes" -> "네더";
            case "the_end" -> "엔드";
            case "cherry_grove" -> "벚꽃 숲";
            case "deep_dark" -> "깊은 어둠";
            default -> bioName.replace("_", " ");
        };
    }
}
