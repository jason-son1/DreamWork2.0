package com.dreamwork.hook;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI 확장 클래스
 * 
 * DreamWork 플레이스홀더를 외부에서 사용할 수 있게 합니다.
 * 스코어보드, 채팅 등에서 %dreamwork_xxx% 형식으로 사용됩니다.
 * 
 * 사용 가능한 플레이스홀더:
 * - %dreamwork_miner_level% : 광부 레벨
 * - %dreamwork_farmer_level% : 농부 레벨
 * - %dreamwork_fisher_level% : 어부 레벨
 * - %dreamwork_hunter_level% : 사냥꾼 레벨
 * - %dreamwork_adventurer_level% : 탐험가 레벨
 * - %dreamwork_total_level% : 총 레벨 합계
 * - %dreamwork_average_level% : 평균 레벨
 * - %dreamwork_highest_job% : 가장 높은 레벨의 직업 이름
 * 
 * @author DreamWork Team
 */
public class DreamWorkExpansion extends PlaceholderExpansion {

    private final DreamWorkPlugin plugin;

    public DreamWorkExpansion(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dreamwork";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // 리로드 시에도 유지
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null)
            return "";

        UserData userData = plugin.getUserDataManager().getUserData(player);

        // 직업별 레벨
        if (identifier.endsWith("_level")) {
            String jobName = identifier.replace("_level", "");
            JobType jobType = JobType.fromConfigKey(jobName);

            if (jobType != null) {
                return String.valueOf(userData.getJobLevel(jobType));
            }
        }

        // 직업별 경험치
        if (identifier.endsWith("_exp")) {
            String jobName = identifier.replace("_exp", "");
            JobType jobType = JobType.fromConfigKey(jobName);

            if (jobType != null) {
                return String.format("%.0f", userData.getJobExp(jobType));
            }
        }

        // 직업별 필요 경험치
        if (identifier.endsWith("_required_exp")) {
            String jobName = identifier.replace("_required_exp", "");
            JobType jobType = JobType.fromConfigKey(jobName);

            if (jobType != null) {
                int level = userData.getJobLevel(jobType);
                double required = plugin.getJobManager().getRequiredExp(level);
                return String.format("%.0f", required);
            }
        }

        // 직업별 진행률 (%)
        if (identifier.endsWith("_percent")) {
            String jobName = identifier.replace("_percent", "");
            JobType jobType = JobType.fromConfigKey(jobName);

            if (jobType != null) {
                int level = userData.getJobLevel(jobType);
                double current = userData.getJobExp(jobType);
                double required = plugin.getJobManager().getRequiredExp(level);
                double percent = (current / required) * 100;
                return String.format("%.1f", percent);
            }
        }

        // 특수 플레이스홀더
        switch (identifier) {
            case "total_level":
                return String.valueOf(userData.getTotalLevels());

            case "average_level":
                return String.format("%.1f", userData.getAverageLevel());

            case "highest_job":
                JobType highest = userData.getHighestLevelJob();
                return highest.getDisplayName();

            case "highest_job_level":
                JobType highestJob = userData.getHighestLevelJob();
                return String.valueOf(userData.getJobLevel(highestJob));

            case "citizen_rank":
                // TODO: 시민 등급 시스템 구현 후 연동
                return "방랑자";

            default:
                return null;
        }
    }
}
