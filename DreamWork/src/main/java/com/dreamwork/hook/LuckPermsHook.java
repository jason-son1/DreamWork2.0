package com.dreamwork.hook;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.job.JobType;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * LuckPerms 권한 시스템 Hook
 * 
 * LuckPerms API를 통해 권한과 칭호(랭크)를 관리합니다.
 * 직업 레벨업 시 자동으로 칭호와 권한을 업데이트합니다.
 * 
 * @author DreamWork Team
 */
public class LuckPermsHook {

    private final DreamWorkPlugin plugin;
    private LuckPerms luckPerms;
    private boolean enabled = false;

    public LuckPermsHook(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    /**
     * LuckPerms 연동 설정
     */
    private void setup() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                luckPerms = LuckPermsProvider.get();
                enabled = true;
                plugin.debug("LuckPerms 연동 완료");
            } catch (Exception e) {
                plugin.log(Level.WARNING, "LuckPerms API 초기화 실패: " + e.getMessage());
                enabled = false;
            }
        } else {
            plugin.debug("LuckPerms 플러그인을 찾을 수 없습니다 (선택적 기능)");
        }
    }

    /**
     * 연동 상태 확인
     */
    public boolean isEnabled() {
        return enabled && luckPerms != null;
    }

    /**
     * 플레이어 랭크/칭호 업데이트
     * 직업 레벨업 시 호출됩니다.
     */
    public void updatePlayerRank(Player player, JobType jobType, int level) {
        if (!isEnabled())
            return;

        // 비동기로 처리
        CompletableFuture.runAsync(() -> {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user == null)
                    return;

                // 레벨 구간별 칭호 결정
                String title = getTitleForLevel(jobType, level);
                int priority = getPriorityForJob(jobType);

                // 기존 직업 칭호 제거 (해당 직업의 prefix만)
                String oldPrefixPattern = getJobPrefixPattern(jobType);
                user.data().clear(node -> {
                    if (node instanceof PrefixNode prefixNode) {
                        return prefixNode.getKey().contains(jobType.getConfigKey());
                    }
                    return false;
                });

                // 새 칭호 추가
                if (title != null && !title.isEmpty()) {
                    PrefixNode prefixNode = PrefixNode.builder(title + " ", priority).build();
                    user.data().add(prefixNode);
                }

                // 레벨 기반 권한 추가
                String permissionNode = "dreamwork.job." + jobType.getConfigKey() + ".level." + level;
                user.data().add(Node.builder(permissionNode).build());

                // 저장
                luckPerms.getUserManager().saveUser(user);

                plugin.debug(player.getName() + " 랭크 업데이트: " + title);

            } catch (Exception e) {
                plugin.log(Level.WARNING, "랭크 업데이트 실패: " + e.getMessage());
            }
        });
    }

    /**
     * 레벨에 따른 칭호 결정
     */
    private String getTitleForLevel(JobType jobType, int level) {
        String jobIcon = jobType.getIcon();
        String jobName = jobType.getDisplayName();

        // 레벨 구간별 칭호
        if (level >= 100) {
            return "§6[§e전설의 " + jobName + "§6]";
        } else if (level >= 75) {
            return "§5[§d대가 " + jobName + "§5]";
        } else if (level >= 50) {
            return "§9[§b숙련 " + jobName + "§9]";
        } else if (level >= 25) {
            return "§2[§a노련한 " + jobName + "§2]";
        } else if (level >= 10) {
            return "§8[§7견습 " + jobName + "§8]";
        } else {
            return "§7[" + jobName + " Lv." + level + "]";
        }
    }

    /**
     * 직업별 칭호 우선순위
     * 여러 직업 중 가장 높은 레벨의 직업 칭호가 보이도록
     */
    private int getPriorityForJob(JobType jobType) {
        return switch (jobType) {
            case MINER -> 100;
            case FARMER -> 101;
            case FISHER -> 102;
            case HUNTER -> 103;
            case ADVENTURER -> 104;
        };
    }

    /**
     * 직업별 prefix 패턴
     */
    private String getJobPrefixPattern(JobType jobType) {
        return "dreamwork.prefix." + jobType.getConfigKey();
    }

    /**
     * 권한 추가
     */
    public void addPermission(Player player, String permission) {
        if (!isEnabled())
            return;

        CompletableFuture.runAsync(() -> {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user == null)
                    return;

                user.data().add(Node.builder(permission).build());
                luckPerms.getUserManager().saveUser(user);

                plugin.debug(player.getName() + " 권한 추가: " + permission);
            } catch (Exception e) {
                plugin.log(Level.WARNING, "권한 추가 실패: " + e.getMessage());
            }
        });
    }

    /**
     * 권한 제거
     */
    public void removePermission(Player player, String permission) {
        if (!isEnabled())
            return;

        CompletableFuture.runAsync(() -> {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user == null)
                    return;

                user.data().remove(Node.builder(permission).build());
                luckPerms.getUserManager().saveUser(user);

                plugin.debug(player.getName() + " 권한 제거: " + permission);
            } catch (Exception e) {
                plugin.log(Level.WARNING, "권한 제거 실패: " + e.getMessage());
            }
        });
    }

    /**
     * 권한 확인
     */
    public boolean hasPermission(Player player, String permission) {
        if (!isEnabled())
            return player.hasPermission(permission);

        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null)
                return player.hasPermission(permission);

            return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
        } catch (Exception e) {
            return player.hasPermission(permission);
        }
    }

    /**
     * LuckPerms API 직접 접근
     */
    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
