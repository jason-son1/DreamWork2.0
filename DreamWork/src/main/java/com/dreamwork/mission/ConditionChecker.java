package com.dreamwork.mission;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * 미션 조건 확인 클래스
 * 
 * 플레이어가 미션을 수행할 수 있는 상태인지 확인합니다.
 * 
 * @author DreamWork Team
 */
public class ConditionChecker {

    private final DreamWorkPlugin plugin;

    public ConditionChecker(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 모든 조건 만족 여부 확인
     */
    public boolean check(Player player, MissionTemplate template, Map<String, Object> context) {
        if (template.getConditions() == null || template.getConditions().isEmpty()) {
            return true;
        }

        for (String condition : template.getConditions()) {
            if (!checkCondition(player, condition, context)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 개별 조건 확인
     */
    private boolean checkCondition(Player player, String condition, Map<String, Object> context) {
        try {
            String[] parts = condition.split(":");
            String type = parts[0].toUpperCase();

            switch (type) {
                // --- 이벤트 컨텍스트 확인 ---
                case "IS_HEADSHOT":
                    return context != null && Boolean.TRUE.equals(context.get("isHeadshot"));

                case "DISTANCE":
                    if (context == null || !context.containsKey("distance"))
                        return false;
                    double dist = (double) context.get("distance");
                    String op = parts[1].substring(0, 1);
                    double val = Double.parseDouble(parts[1].substring(1));
                    if (op.equals(">"))
                        return dist > val;
                    if (op.equals("<"))
                        return dist < val;
                    return dist == val;

                case "IS_BABY":
                    return context != null
                            && Boolean.parseBoolean(parts[1]) == Boolean.TRUE.equals(context.get("isBaby"));

                case "NO_EXPLOSION":
                    return context != null && Boolean.TRUE.equals(context.get("noExplosion"));

                case "REPLANT":
                    return context != null && Boolean.TRUE.equals(context.get("replant"));

                case "ENCHANT":
                    if (context == null || !context.containsKey("enchants"))
                        return false;
                    Map<?, ?> enchants = (Map<?, ?>) context.get("enchants");
                    return enchants.containsKey(org.bukkit.enchantments.Enchantment
                            .getByKey(org.bukkit.NamespacedKey.minecraft(parts[1].toLowerCase())));

                case "WEATHER":
                    if ("STORM".equals(parts[1]))
                        return player.getWorld().hasStorm();
                    if ("CLEAR".equals(parts[1]))
                        return !player.getWorld().hasStorm();
                    return true;

                case "TIME":
                    long time = player.getWorld().getTime();
                    if ("NIGHT".equals(parts[1]))
                        return time > 13000 && time < 23000;
                    if ("DAY".equals(parts[1]))
                        return time >= 0 && time <= 13000;
                    return true;

                case "BAIT":
                    return context != null && parts[1].equalsIgnoreCase((String) context.get("bait"));

                case "PDC":
                    if (context == null || !context.containsKey("pdc"))
                        return false;
                    Map<?, ?> pdc = (Map<?, ?>) context.get("pdc");
                    String[] tag = parts[1].split("=");
                    return pdc.containsKey(tag[0]) && pdc.get(tag[0]).equals(tag[1]);

                // --- 플레이어 상태 확인 ---
                case "HP_BELOW":
                    // 체력 확인 (하트 칸 수)
                    double limit = Double.parseDouble(parts[1]) * 2; // 하트 1칸 = 2 HP
                    return player.getHealth() <= limit;

                case "IN_LAVA":
                    // 용암 속에 있는지 확인 (눈 높이 기준)
                    return player.getEyeLocation().getBlock().getType() == Material.LAVA;

                case "LIGHT_LEVEL_BELOW":
                    // 밝기 확인
                    int lightLimit = Integer.parseInt(parts[1]);
                    return player.getLocation().getBlock().getLightLevel() <= lightLimit;

                case "BIOME":
                    // 바이옴 확인
                    String biomeName = parts[1].toUpperCase();
                    return player.getLocation().getBlock().getBiome().name().equals(biomeName);

                case "SKILL_ACTIVE":
                    // 스킬 활성화 여부 확인
                    // TODO: SkillManager 연동
                    // return plugin.getSkillManager().isSkillActive(player, parts[1]);
                    return true; // 임시 통과

                case "TIME_WITHIN":
                    // 시간 제한 (리스너에서 별도 처리 필요, 여기서는 정적 상태만 체크 어려움)
                    // 이 조건은 이벤트 발생 시점에 타임스탬프 비교로 처리해야 함
                    return true;

                case "NEARBY_ENTITY":
                    // 주변 엔티티 확인 (반경:엔티티타입)
                    int radius = Integer.parseInt(parts[1]);
                    EntityType entityType = EntityType.valueOf(parts[2].toUpperCase());
                    for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                        if (entity.getType() == entityType) {
                            return true;
                        }
                    }
                    return false;

                case "IS_SNEAKING":
                    return player.isSneaking();

                case "IS_SPRINTING":
                    return player.isSprinting();

                default:
                    // 알 수 없는 조건은 무시하거나 false 처리
                    return true;
            }
        } catch (Exception e) {
            // 조건 파싱 실패 시
            return false;
        }
    }
}
