package com.dreamwork.job.listener;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import com.dreamwork.mission.MissionType;
import com.dreamwork.skill.impl.MinersTrance;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * 광부 직업 리스너
 * 
 * BlockBreakEvent를 감지하여 광물 채굴 시 경험치와 돈을 지급합니다.
 * Plan에 명시된 Tier 시스템 및 특수 보상 공식을 따릅니다.
 * 
 * @author DreamWork Team
 */
public class MinerListener implements Listener {

    private final DreamWorkPlugin plugin;
    private final Random random = new Random();
    private MinersTrance minersTrance; // Injected or retrieved

    public MinerListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * MinersTrance 참조 설정
     */
    public void setMinersTrance(MinersTrance minersTrance) {
        this.minersTrance = minersTrance;
    }

    // Helper to get trance if not set (fallback)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        if (!event.canBuild())
            return;

        Block block = event.getBlockPlaced();
        // 광물인 경우에만 영속적 데이터 저장
        String type = block.getType().name();
        if (type.contains("ORE") || type.contains("ANCIENT_DEBRIS")) {
            plugin.getUserDataManager().recordPlacedBlock(block.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // 어뷰징 방지: 플레이어가 설치한 블록인지 확인
        boolean isPlaced = plugin.getUserDataManager().isPlacedBlock(block.getLocation());

        // 블록이 설치된 것이었다면 기록 제거 및 보상 미지급
        if (isPlaced) {
            plugin.getUserDataManager().removePlacedBlock(block.getLocation());
            return;
        }

        // 0. Towny 지역 확인 (남의 땅 채굴 방지)
        if (plugin.getTownyHook().isEnabled() && !plugin.getTownyHook().isInOwnTown(player, block.getLocation())) {
            // 야생(Wilderness)이거나 자신의 타운이 아니면 직업 보상 미지급
            // 단, 야생 채굴 허용 여부는 기획에 따라 다를 수 있음. 여기선 "타운 내부 방지"에 초점.
            // 보통 남의 타운이면 BlockBreakEvent 자체가 취소되지만,
            // 취소되지 않은 경우(권한 있음)에도 직업 exp는 주지 않으려면 체크.
            // 기획서: "타운 내부가 아니면 ... 남의 땅에서 스킬 발동 불가"
            // 여기서는 getTownName이 있고 자신의 타운이 아니면 리턴.
            String townName = plugin.getTownyHook().getTownName(block.getLocation());
            if (townName != null) {
                // 타운 내부인데
                if (!plugin.getTownyHook().isInOwnTown(player, block.getLocation())) {
                    return; // 남의 타운이면 보상 없음
                }
            }
        }

        Material material = block.getType();
        String materialKey = material.name().toLowerCase();

        // 직업 설정 가져오기
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig("miner");
        if (jobConfig == null)
            return;

        double exp = 0;
        double money = 0;
        boolean broadcast = false;

        // 1. 보상 테이블 조회 (Tier System)
        // YAML 구조: rewards.<material_key>.exp / .money
        String configPath = "rewards." + materialKey;
        if (jobConfig.contains(configPath)) {
            exp = jobConfig.getDouble(configPath + ".exp", 0);
            money = jobConfig.getDouble(configPath + ".money", 0);
            broadcast = jobConfig.getBoolean(configPath + ".broadcast", false);
        } else {
            // 기본값 (돌 등 Tier 0)
            // 매크로 방지: 아주 미미한 경험치
            if (materialKey.contains("stone") || materialKey.contains("deepslate")
                    || materialKey.contains("netherrack")) {
                exp = 0.1;
            }
        }

        if (exp <= 0 && money <= 0)
            return;

        // 쿨다운 체크 (Anti-Abuse)
        var userData = plugin.getUserDataManager().getUserData(player);
        long cooldown = plugin.getConfigManager().getConfig()
                .getLong("anti-abuse.action-cooldown", 500); // 0.5s

        if (!userData.checkAndUpdateCooldown("mine_" + materialKey, cooldown)) {
            return; // 쿨다운 중
        }

        // 2. 광부의 몰입 (Trance) 보너스 계산
        // 공식: P_drop = P_base * (1 + Level * 0.01) * Multiplier_trance
        double tranceExpMultiplier = 1.0;

        // Retrieve skill instance dynamically if needed
        // Assuming we registered it in DreamWorkPlugin, we can try to get it,
        // or effectively rely on the fact that we can instantiate a helper or use a
        // static map.
        // But since we are inside Listener, let's use the setter or plugin access.
        // For this implementation, I will rely on the `handleSpecialDrops` to apply
        // trance bonus for drops,
        // and here for Exp.

        // Note: Ideally DreamWorkPlugin should expose getMinersTrance().
        // I will assume getMinersTrance() returns proper object if set.

        if (minersTrance != null) {
            tranceExpMultiplier = minersTrance.getExpMultiplier(player);
        }

        double finalExp = exp * tranceExpMultiplier;

        // 3. 보상 지급
        plugin.getJobManager().giveReward(player, JobType.MINER, finalExp, money);

        // 4. 미션 이벤트 트리거
        plugin.getMissionManager().processEvent(player, MissionType.BREAK, materialKey, 1);

        // 5. 서버 알림 (전설 광물)
        if (broadcast) {
            String msg = "§e[§6DreamWork§e] §f" + player.getName() + "님이 §b" + material.name() + "§f을(를) 발견했습니다!";
            Bukkit.broadcastMessage(msg);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        }

        // 6. 특수 아이템 드롭 (드림스톤, 미지의 광석)
        handleSpecialDrops(player, materialKey, jobConfig);

        plugin.debug(player.getName() + " 채굴: " + materialKey + " (Exp: " + finalExp + ", Money: " + money + ")");
    }

    /**
     * 특수 아이템 드롭 처리
     * 공식: P_drop = P_base * (1 + Level * 0.01) * Multiplier_trance
     */
    private void handleSpecialDrops(Player player, String materialKey, FileConfiguration config) {
        // 드롭 대상이 아니면 스킵 (Tier 2 이상부터 권장, 여기선 Config에 정의된 확률에 따름)
        // 하지만 기획서상 "광물 발견" 시 드롭이므로, Stone은 제외해야 함.
        if (materialKey.contains("stone") && !materialKey.contains("ore"))
            return;
        if (materialKey.contains("deepslate") && !materialKey.contains("ore"))
            return;

        int level = plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.MINER);
        double tranceMult = (minersTrance != null) ? minersTrance.getRareDropMultiplier(player) : 1.0;

        // 1. 드림 스톤
        double dreamBase = config.getDouble("drops.dream_stone.base_chance", 0.001);
        double dreamChance = dreamBase * (1 + level * 0.01) * tranceMult;

        if (random.nextDouble() < dreamChance) {
            ItemStack item = plugin.getItemManager().createItem("dream_stone", 1);
            if (item != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                Bukkit.broadcastMessage("§e[§6DreamWork§e] §d" + player.getName() + "님이 전설의 재료, 드림 스톤을 발견했습니다!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
            }
        }

        // 2. 미지의 광석
        double unknownBase = config.getDouble("drops.unknown_ore.base_chance", 0.05);
        double unknownChance = unknownBase * (1 + level * 0.01) * tranceMult;

        if (random.nextDouble() < unknownChance) {
            ItemStack item = plugin.getItemManager().createItem("unknown_ore", 1);
            // Config에 unknown_ore가 없으면 기본 돌을 사용해서라도 만듦
            if (item == null) {
                item = new ItemStack(Material.COBBLED_DEEPSLATE);
            }

            // PDC 태그 부착 (핵심 로직)
            item = plugin.getItemManager().setUnidentified(item);

            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage("§7[광부] 미지의 광석을 발견했습니다.");
        }
    }

    /**
     * 플레이어 상호작용 이벤트 - 광맥 탐지 스킬 사용
     */
    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
                event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.getType().name().contains("PICKAXE")) {
            return;
        }

        // 광부 직업인지 확인
        if (plugin.getUserDataManager().getUserData(player).getJobLevel(JobType.MINER) <= 0) {
            return;
        }

        // 광맥 탐지 스킬 사용
        new com.dreamwork.skill.impl.OreRadar(plugin).use(player);
    }
}
