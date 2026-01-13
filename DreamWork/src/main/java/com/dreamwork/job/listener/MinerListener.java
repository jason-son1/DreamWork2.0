package com.dreamwork.job.listener;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import com.dreamwork.mission.MissionType;
import com.dreamwork.skill.impl.MinersTrance;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

/**
 * 광부 직업 리스너
 * 
 * BlockBreakEvent를 감지하여 광물 채굴 시 경험치와 돈을 지급합니다.
 * 
 * @author DreamWork Team
 */
public class MinerListener implements Listener {

    private final DreamWorkPlugin plugin;
    private final NamespacedKey placedByPlayerKey;
    private final Random random = new Random();
    private MinersTrance minersTrance;

    public MinerListener(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        this.placedByPlayerKey = new NamespacedKey(plugin, "placed_by_player");
    }

    /**
     * MinersTrance 참조 설정 (DreamWorkPlugin에서 호출)
     */
    public void setMinersTrance(MinersTrance minersTrance) {
        this.minersTrance = minersTrance;
    }

    /**
     * 블록 설치 이벤트 - 플레이어가 설치한 블록 표시
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // 설치된 블록 감지 옵션 확인
        if (!plugin.getConfigManager().getConfig()
                .getBoolean("jobs.check-player-placed-blocks", true)) {
            return;
        }

        // TODO: 블록의 메타데이터에 표시 (TileState 사용)
        // 현재는 간단히 건너뜀
    }

    /**
     * 블록 파괴 이벤트 - 광물 채굴 처리
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();

        // 직업 설정 가져오기
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig("miner");
        if (jobConfig == null)
            return;

        // 블록별 보상 확인
        ConfigurationSection materialsSection = jobConfig.getConfigurationSection("materials");
        if (materialsSection == null)
            return;

        String materialKey = material.name().toLowerCase();
        ConfigurationSection blockConfig = materialsSection.getConfigurationSection(materialKey);

        if (blockConfig == null)
            return;

        // 경험치 및 돈 가져오기
        double exp = blockConfig.getDouble("exp", 0);
        double money = blockConfig.getDouble("money", 0);

        if (exp <= 0 && money <= 0)
            return;

        // 쿨다운 체크
        var userData = plugin.getUserDataManager().getUserData(player);
        long cooldown = plugin.getConfigManager().getConfig()
                .getLong("anti-abuse.action-cooldown", 500);

        if (!userData.checkAndUpdateCooldown("mine_" + materialKey, cooldown)) {
            return; // 쿨다운 중
        }

        // 광부의 몰입 보너스 적용
        double expBonus = 1.0;
        if (minersTrance != null) {
            expBonus += minersTrance.getExpBonus(player);
        }

        double finalExp = exp * expBonus;

        // 보상 지급
        plugin.getJobManager().giveReward(player, JobType.MINER, finalExp, money);

        // 미션 이벤트 트리거
        plugin.getMissionManager().processEvent(player, MissionType.BREAK, materialKey, 1);

        // 특수 이벤트: 미지의 광석 드롭 (다이아몬드/에메랄드/금/레드스톤 채굴 시)
        handleSpecialDrops(player, material);

        plugin.debug(player.getName() + " 채굴: " + material.name() +
                " (Exp: " + exp + ", Money: " + money + ")");
    }

    /**
     * 특수 드롭 처리 (미지의 광석, 드림스톤 등)
     */
    private void handleSpecialDrops(Player player, Material material) {
        FileConfiguration jobConfig = plugin.getConfigManager().getJobConfig("miner");
        if (jobConfig == null)
            return;

        // 희귀 광물에서만 특수 드롭
        if (!isRareMineral(material))
            return;

        // 미지의 광석 드롭 확률 (몰입 보너스 적용)
        double unknownOreChance = jobConfig.getDouble("special_drops.unknown_ore_chance", 0.05);

        if (minersTrance != null) {
            unknownOreChance += minersTrance.getRareChanceBonus(player);
        }

        if (random.nextDouble() < unknownOreChance) {
            ItemStack unknownOre = plugin.getItemManager().createItem("unknown_ore", 1);
            if (unknownOre != null) {
                player.getInventory().addItem(unknownOre).values()
                        .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

                String message = plugin.getConfigManager().getMessage("miner.unknown-ore-found");
                player.sendMessage(plugin.getConfigManager().getMessage("prefix") + message);
            }
        }

        // 드림스톤 드롭 확률 (매우 희귀, 몰입 보너스 적용)
        double dreamstoneChance = jobConfig.getDouble("special_drops.dreamstone_chance", 0.005);

        if (minersTrance != null) {
            dreamstoneChance += minersTrance.getRareChanceBonus(player);
        }

        if (random.nextDouble() < dreamstoneChance) {
            ItemStack dreamstone = plugin.getItemManager().createItem("dreamstone", 1);
            if (dreamstone != null) {
                player.getInventory().addItem(dreamstone).values()
                        .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

                String message = plugin.getConfigManager().getMessage("miner.dreamstone-found");
                player.sendMessage(plugin.getConfigManager().getMessage("prefix") + message);
            }
        }
    }

    /**
     * 희귀 광물인지 확인
     */
    private boolean isRareMineral(Material material) {
        return switch (material) {
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                    EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                    GOLD_ORE, DEEPSLATE_GOLD_ORE,
                    REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                    LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                    ANCIENT_DEBRIS ->
                true;
            default -> false;
        };
    }
}
