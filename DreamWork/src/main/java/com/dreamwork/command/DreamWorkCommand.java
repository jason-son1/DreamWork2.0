package com.dreamwork.command;

import com.dreamwork.DreamWorkPlugin;
import com.dreamwork.core.UserData;
import com.dreamwork.job.JobType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DreamWork 메인 명령어 처리기
 * 
 * /dw, /dream, /dreamwork 명령어를 처리합니다.
 * 
 * 하위 명령어:
 * - /dw : 메인 대시보드 GUI 열기
 * - /dw reload : 설정 리로드 (관리자)
 * - /dw give <아이템ID> [수량] [플레이어] : 아이템 지급 (관리자)
 * - /dw setlevel <직업> <레벨> [플레이어] : 레벨 설정 (관리자)
 * - /dw info [플레이어] : 직업 정보 확인
 * 
 * @author DreamWork Team
 */
public class DreamWorkCommand implements CommandExecutor, TabCompleter {

    private final DreamWorkPlugin plugin;

    public DreamWorkCommand(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 인자가 없으면 GUI 열기 (플레이어만)
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("general.player-only"));
                return true;
            }

            plugin.getGuiManager().openMainDashboard(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);

            case "give":
                return handleGive(sender, args);

            case "setlevel":
                return handleSetLevel(sender, args);

            case "info":
                return handleInfo(sender, args);

            case "debug":
                return handleDebug(sender, args);

            case "help":
            default:
                return handleHelp(sender);
        }
    }

    /**
     * /dw reload - 설정 리로드
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("dreamwork.admin.reload")) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("general.no-permission"));
            return true;
        }

        try {
            plugin.reload();
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("general.reload-success"));
        } catch (Exception e) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("error", e.getMessage());
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("general.reload-failed"));
        }

        return true;
    }

    /**
     * /dw give <아이템ID> [수량] [플레이어] - 아이템 지급
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dreamwork.admin.give")) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("general.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c사용법: /dw give <아이템ID> [수량] [플레이어]");
            return true;
        }

        String itemId = args[1];
        int amount = 1;
        Player target;

        // 수량 파싱
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("general.invalid-amount"));
                return true;
            }
        }

        // 대상 플레이어
        if (args.length >= 4) {
            target = Bukkit.getPlayer(args[3]);
            if (target == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", args[3]);
                sender.sendMessage(plugin.getConfigManager()
                        .getMessage("general.player-not-found", placeholders));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§c콘솔에서는 플레이어를 지정해야 합니다.");
            return true;
        }

        // 아이템 생성 및 지급
        var item = plugin.getItemManager().createItem(itemId, amount);
        if (item == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", itemId);
            sender.sendMessage(plugin.getConfigManager()
                    .getMessage("item.item-not-found", placeholders));
            return true;
        }

        target.getInventory().addItem(item);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("item", itemId);
        placeholders.put("amount", String.valueOf(amount));
        sender.sendMessage(plugin.getConfigManager()
                .getMessage("item.give-success", placeholders));

        return true;
    }

    /**
     * /dw setlevel <직업> <레벨> [플레이어] - 레벨 설정
     */
    private boolean handleSetLevel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dreamwork.admin.setlevel")) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("general.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§c사용법: /dw setlevel <직업> <레벨> [플레이어]");
            return true;
        }

        JobType jobType = JobType.fromString(args[1]);
        if (jobType == null) {
            sender.sendMessage("§c알 수 없는 직업: " + args[1]);
            sender.sendMessage("§7사용 가능: miner, farmer, fisher, hunter, adventurer");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
            level = Math.max(1, Math.min(level, plugin.getJobManager().getMaxLevel()));
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("general.invalid-amount"));
            return true;
        }

        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayer(args[3]);
            if (target == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", args[3]);
                sender.sendMessage(plugin.getConfigManager()
                        .getMessage("general.player-not-found", placeholders));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§c콘솔에서는 플레이어를 지정해야 합니다.");
            return true;
        }

        UserData userData = plugin.getUserDataManager().getUserData(target);
        userData.setJobLevel(jobType, level);
        userData.setJobExp(jobType, 0);

        sender.sendMessage("§a" + target.getName() + "의 " + jobType.getDisplayName() +
                " 레벨을 " + level + "로 설정했습니다.");

        return true;
    }

    /**
     * /dw info [플레이어] - 직업 정보 확인
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        Player target;

        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", args[1]);
                sender.sendMessage(plugin.getConfigManager()
                        .getMessage("general.player-not-found", placeholders));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§c사용법: /dw info <플레이어>");
            return true;
        }

        UserData userData = plugin.getUserDataManager().getUserData(target);

        sender.sendMessage("§6===== " + target.getName() + "의 직업 정보 =====");

        for (JobType jobType : JobType.values()) {
            int level = userData.getJobLevel(jobType);
            double exp = userData.getJobExp(jobType);
            double required = plugin.getJobManager().getRequiredExp(level);
            double percent = (exp / required) * 100;

            sender.sendMessage(String.format("§e%s %s: §fLv.%d §7(%.0f/%.0f - %.1f%%)",
                    jobType.getIcon(), jobType.getDisplayName(),
                    level, exp, required, percent));
        }

        sender.sendMessage("§7평균 레벨: §f" + String.format("%.1f", userData.getAverageLevel()));
        sender.sendMessage("§7총 레벨: §f" + userData.getTotalLevels());

        return true;
    }

    /**
     * /dw debug - 디버그 명령어 (개발용)
     */
    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dreamwork.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("general.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c사용법: /dw debug <exp|money|check>");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefixedMessage("general.player-only"));
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "exp":
                // 경험치 테스트
                if (args.length >= 4) {
                    JobType job = JobType.fromString(args[2]);
                    double exp = Double.parseDouble(args[3]);
                    if (job != null) {
                        plugin.getJobManager().addExperience(player, job, exp);
                        sender.sendMessage("§a" + job.getDisplayName() + " 경험치 +" + exp);
                    }
                }
                break;

            case "money":
                // 돈 테스트
                if (args.length >= 3) {
                    double money = Double.parseDouble(args[2]);
                    plugin.getJobManager().giveMoney(player, money);
                    sender.sendMessage("§a$" + money + " 지급됨");
                }
                break;

            case "check":
                // 손에 든 아이템 정보
                var item = player.getInventory().getItemInMainHand();
                if (item.getType().isAir()) {
                    sender.sendMessage("§c아이템을 들고 있지 않습니다.");
                } else {
                    String itemId = plugin.getItemManager().getDreamItemId(item);
                    sender.sendMessage("§6아이템 정보:");
                    sender.sendMessage("§7Material: §f" + item.getType().name());
                    sender.sendMessage("§7DreamWork ID: §f" + (itemId != null ? itemId : "없음 (바닐라 아이템)"));
                    if (itemId != null) {
                        int quality = plugin.getItemManager().getQuality(item);
                        sender.sendMessage("§7품질: §f" + quality + "성");
                    }
                }
                break;

            default:
                sender.sendMessage("§c알 수 없는 디버그 명령어: " + action);
        }

        return true;
    }

    /**
     * /dw help - 도움말
     */
    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage("§6===== DreamWork 명령어 =====");
        sender.sendMessage("§e/dw §7- 메인 대시보드 GUI 열기");
        sender.sendMessage("§e/dw info [플레이어] §7- 직업 정보 확인");

        if (sender.hasPermission("dreamwork.admin")) {
            sender.sendMessage("§e/dw reload §7- 설정 리로드");
            sender.sendMessage("§e/dw give <아이템ID> [수량] [플레이어] §7- 아이템 지급");
            sender.sendMessage("§e/dw setlevel <직업> <레벨> [플레이어] §7- 레벨 설정");
            sender.sendMessage("§e/dw debug <exp|money|check> §7- 디버그 기능");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(List.of("help", "info"));

            if (sender.hasPermission("dreamwork.admin")) {
                subCommands.addAll(List.of("reload", "give", "setlevel", "debug"));
            }

            String input = args[0].toLowerCase();
            completions = subCommands.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();

            switch (subCommand) {
                case "setlevel":
                    completions = Arrays.stream(JobType.values())
                            .map(JobType::getConfigKey)
                            .filter(s -> s.startsWith(input))
                            .collect(Collectors.toList());
                    break;

                case "give":
                    // TODO: 아이템 ID 목록 반환
                    completions = List.of("unknown_ore", "dreamstone", "potato_3star");
                    break;

                case "info":
                    completions = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(s -> s.toLowerCase().startsWith(input))
                            .collect(Collectors.toList());
                    break;

                case "debug":
                    completions = List.of("exp", "money", "check").stream()
                            .filter(s -> s.startsWith(input))
                            .collect(Collectors.toList());
                    break;
            }
        } else if (args.length == 3) {
            // 세 번째 인자: 숫자 또는 플레이어
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("setlevel")) {
                completions = List.of("1", "10", "25", "50", "100");
            } else if (subCommand.equals("debug") && args[1].equals("exp")) {
                completions = Arrays.stream(JobType.values())
                        .map(JobType::getConfigKey)
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
