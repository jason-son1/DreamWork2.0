package com.dreamwork.job.listener;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import com.dreamwork.mission.MissionType;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * 탐험가 직업 리스너
 * 
 * 새로운 청크/바이옴 발견 시 경험치를 지급합니다.
 * 
 * @author DreamWork Team
 */
public class AdventurerListener implements Listener {

    private final DreamWorkPlugin plugin;

    public AdventurerListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 플레이어 이동 이벤트 - 청크 이동 감지
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 청크가 변경되지 않았으면 무시 (최적화)
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }

        Player player = event.getPlayer();
        Chunk newChunk = event.getTo().getChunk();
        World world = newChunk.getWorld();

        // 이미 방문한 청크인지 확인
        if (plugin.getUserDataManager().hasVisitedChunk(
                player.getUniqueId(),
                world.getName(),
                newChunk.getX(),
                newChunk.getZ())) {
            return;
        }

        // 직업 설정 가져오기
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig("adventurer");
        if (jobConfig == null)
            return;

        // 새 청크 발견 보상
        double chunkExp = jobConfig.getDouble("chunk_discover_exp", 1.0);
        double chunkMoney = jobConfig.getDouble("chunk_discover_money", 0.5);

        // 현재 바이옴 확인
        Biome biome = newChunk.getBlock(8, 64, 8).getBiome();
        String biomeKey = biome.getKey().getKey(); // 1.21.x에서는 getKey() 사용

        // 바이옴별 추가 보상
        if (jobConfig.isConfigurationSection("biomes." + biomeKey)) {
            double biomeExp = jobConfig.getDouble("biomes." + biomeKey + ".exp", 0);
            double biomeMoney = jobConfig.getDouble("biomes." + biomeKey + ".money", 0);

            if (biomeExp > 0 || biomeMoney > 0) {
                chunkExp += biomeExp;
                chunkMoney += biomeMoney;

                // 새 바이옴 발견 메시지
                sendBiomeDiscoveryMessage(player, biome);
            }
        }

        // 보상 지급
        if (chunkExp > 0 || chunkMoney > 0) {
            plugin.getJobManager().giveReward(player, JobType.ADVENTURER, chunkExp, chunkMoney);
        }

        // 청크 방문 기록
        plugin.getUserDataManager().markChunkVisited(
                player.getUniqueId(),
                world.getName(),
                newChunk.getX(),
                newChunk.getZ());

        // 미션 이벤트 트리거
        plugin.getMissionManager().processEvent(player, MissionType.WALK, biomeKey, 1);
    }

    /**
     * 바이옴 발견 메시지
     */
    private void sendBiomeDiscoveryMessage(Player player, Biome biome) {
        String biomeName = getBiomeDisplayName(biome);

        String message = plugin.getConfigManager()
                .getMessage("adventurer.biome-discovered")
                .replace("{biome}", biomeName);

        player.sendMessage(plugin.getConfigManager().getMessage("prefix") + message);
    }

    /**
     * 바이옴 표시 이름 (한국어)
     * 1.21.x에서는 Biome이 Registry 기반이므로 getKey() 사용
     */
    private String getBiomeDisplayName(Biome biome) {
        String key = biome.getKey().getKey(); // minecraft:plains -> plains

        return switch (key) {
            case "plains" -> "평원";
            case "forest" -> "숲";
            case "birch_forest" -> "자작나무 숲";
            case "dark_forest" -> "어두운 숲";
            case "jungle" -> "정글";
            case "desert" -> "사막";
            case "ocean", "deep_ocean" -> "바다";
            case "river" -> "강";
            case "beach" -> "해변";
            case "swamp" -> "늪지대";
            case "taiga" -> "타이가";
            case "snowy_plains" -> "눈덮인 평원";
            case "windswept_hills" -> "산악";
            case "badlands" -> "악지지형";
            case "mushroom_fields" -> "버섯 섬";
            case "the_end" -> "디 엔드";
            case "nether_wastes" -> "네더 황무지";
            case "soul_sand_valley" -> "소울 샌드 계곡";
            case "crimson_forest" -> "진홍빛 숲";
            case "warped_forest" -> "뒤틀린 숲";
            case "basalt_deltas" -> "현무암 삼각지";
            case "deep_dark" -> "깊은 어둠";
            case "lush_caves" -> "무성한 동굴";
            case "dripstone_caves" -> "종유석 동굴";
            default -> key.replace("_", " ");
        };
    }
}
