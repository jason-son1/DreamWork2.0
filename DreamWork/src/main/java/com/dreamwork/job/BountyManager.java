package com.dreamwork.job;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 현상수배 관리자
 * 
 * 사냥꾼 직업을 위한 일일 현상수배 시스템을 관리합니다.
 * 매일 자정에 타겟이 변경되거나, 플레이어별로 할당될 수 있습니다.
 * 여기서는 간단하게 전역 일일 타겟 방식을 사용합니다.
 */
public class BountyManager {

    private final DreamWorkPlugin plugin;
    private EntityType currentTarget;
    private double rewardMultiplier;
    private long nextResetTime;

    private final List<EntityType> bountyCandidates = Arrays.asList(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER,
            EntityType.ENDERMAN, EntityType.WITCH, EntityType.DROWNED, EntityType.PILLAGER);

    public BountyManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        refreshBounty();
    }

    /**
     * 현상수배 갱신
     */
    public void refreshBounty() {
        Random random = new Random();
        this.currentTarget = bountyCandidates.get(random.nextInt(bountyCandidates.size()));
        this.rewardMultiplier = 1.5 + (random.nextDouble() * 1.0); // 1.5 ~ 2.5배

        // 다음 자정까지 시간 계산 (여기서는 단순히 24시간 후로 설정하거나 더미 값)
        this.nextResetTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24H

        plugin.getLogger().info(
                "New Bounty Target: " + currentTarget.name() + " (x" + String.format("%.2f", rewardMultiplier) + ")");
    }

    public EntityType getCurrentTarget() {
        if (System.currentTimeMillis() > nextResetTime) {
            refreshBounty();
        }
        return currentTarget;
    }

    public double getRewardMultiplier() {
        return rewardMultiplier;
    }

    /**
     * 현상수배 전단지 아이템 생성 (GUI용)
     */
    public ItemStack getBountyItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c[ ☠ 현상수배 ]");

        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("§cTarget: §f" + currentTarget.name()); // 한글화 필요 시 변환 로직 추가
        lore.add("§6Reward: §f" + String.format("%.1fx", rewardMultiplier));
        lore.add("§7");
        lore.add("§7이 몬스터를 처치하면");
        lore.add("§7더 많은 보상을 받습니다.");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
