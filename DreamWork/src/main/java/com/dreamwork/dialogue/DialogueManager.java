package com.dreamwork.dialogue;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * 대화 관리자
 * 
 * NPC 대화 데이터를 로드하고 대화 진행을 관리합니다.
 * 
 * @author DreamWork Team
 */
public class DialogueManager {

    private final DreamWorkPlugin plugin;
    private final Map<String, DialogueTree> dialogues = new HashMap<>();

    // 플레이어별 현재 대화 상태
    private final Map<UUID, DialogueSession> activeSessions = new HashMap<>();

    public DialogueManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;
        loadDialogues();
    }

    /**
     * 대화 스크립트 로드
     */
    public void loadDialogues() {
        dialogues.clear();

        File dialogueFolder = new File(plugin.getDataFolder(), "dialogues");
        if (!dialogueFolder.exists()) {
            dialogueFolder.mkdirs();
            // 기본 대화 파일 생성
            createDefaultDialogues(dialogueFolder);
        }

        File[] files = dialogueFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                for (String npcId : config.getKeys(false)) {
                    DialogueTree tree = parseDialogueTree(npcId, config.getConfigurationSection(npcId));
                    if (tree != null) {
                        dialogues.put(npcId, tree);
                        plugin.debug("대화 로드: " + npcId);
                    }
                }
            } catch (Exception e) {
                plugin.log(Level.WARNING, "대화 파일 로드 실패: " + file.getName() + " - " + e.getMessage());
            }
        }

        plugin.log(Level.INFO, "총 " + dialogues.size() + "개의 NPC 대화 스크립트 로드됨");
    }

    /**
     * 대화 트리 파싱
     */
    private DialogueTree parseDialogueTree(String npcId, ConfigurationSection section) {
        if (section == null)
            return null;

        String startNode = section.getString("start", "greeting");
        ConfigurationSection nodesSection = section.getConfigurationSection("nodes");
        if (nodesSection == null)
            return null;

        Map<String, DialogueNode> nodes = new HashMap<>();

        for (String nodeId : nodesSection.getKeys(false)) {
            ConfigurationSection nodeSection = nodesSection.getConfigurationSection(nodeId);
            if (nodeSection == null)
                continue;

            String speaker = nodeSection.getString("speaker", npcId);
            String text = nodeSection.getString("text", "");

            List<DialogueOption> options = new ArrayList<>();
            List<?> optionsList = nodeSection.getList("options");

            if (optionsList != null) {
                for (Object optObj : optionsList) {
                    if (optObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> optMap = (Map<String, Object>) optObj;

                        String optText = (String) optMap.getOrDefault("text", "...");
                        String next = (String) optMap.get("next");
                        String action = (String) optMap.get("action");
                        String condition = (String) optMap.get("condition");

                        options.add(new DialogueOption(optText, next, action, condition));
                    }
                }
            }

            nodes.put(nodeId, new DialogueNode(nodeId, speaker, text, options));
        }

        return new DialogueTree(npcId, startNode, nodes);
    }

    /**
     * 대화 시작
     */
    public void startDialogue(Player player, String npcId) {
        DialogueTree tree = dialogues.get(npcId);
        if (tree == null) {
            plugin.debug("대화 스크립트 없음: " + npcId);
            return;
        }

        DialogueNode startNode = tree.getStartNode();
        if (startNode == null) {
            plugin.debug("시작 노드 없음: " + npcId);
            return;
        }

        // 세션 생성
        DialogueSession session = new DialogueSession(player, tree, startNode);
        activeSessions.put(player.getUniqueId(), session);

        // GUI 열기
        plugin.getGuiManager().openDialogueGui(player, session);
    }

    /**
     * 선택지 선택 처리
     */
    public void selectOption(Player player, int optionIndex) {
        DialogueSession session = activeSessions.get(player.getUniqueId());
        if (session == null)
            return;

        DialogueNode currentNode = session.getCurrentNode();
        List<DialogueOption> options = currentNode.getOptions();

        if (optionIndex < 0 || optionIndex >= options.size())
            return;

        DialogueOption selected = options.get(optionIndex);

        // 액션 실행
        if (selected.hasAction()) {
            executeAction(player, selected.getAction());
        }

        // 다음 노드로 이동
        if (selected.hasNextNode()) {
            DialogueNode nextNode = session.getTree().getNode(selected.getNextNodeId());
            if (nextNode != null) {
                session.setCurrentNode(nextNode);
                plugin.getGuiManager().openDialogueGui(player, session);
                return;
            }
        }

        // 다음 노드가 없으면 대화 종료
        endDialogue(player);
    }

    /**
     * 대화 종료
     */
    public void endDialogue(Player player) {
        activeSessions.remove(player.getUniqueId());
        player.closeInventory();
    }

    /**
     * 액션 실행
     */
    private void executeAction(Player player, String action) {
        if (action == null || action.isEmpty())
            return;

        String[] parts = action.split(":", 2);
        String actionType = parts[0].toUpperCase();
        String actionValue = parts.length > 1 ? parts[1] : "";

        switch (actionType) {
            case "OPEN_GUI" -> {
                switch (actionValue) {
                    case "forge" -> plugin.getGuiManager().openForgeGui(player);
                    case "kitchen" -> plugin.getGuiManager().openKitchenGui(player);
                    case "fishmarket" -> plugin.getGuiManager().openFishMarketGui(player);
                    case "mapstore" -> plugin.getGuiManager().openMapStoreGui(player);
                    case "mission" -> plugin.getGuiManager().openMissionBoardGui(player);
                }
            }
            case "ACCEPT_MISSION" -> {
                plugin.getMissionManager().acceptMission(player, actionValue);
            }
            case "GIVE_ITEM" -> {
                String[] itemParts = actionValue.split(":");
                String itemId = itemParts[0];
                int amount = itemParts.length > 1 ? Integer.parseInt(itemParts[1]) : 1;
                var item = plugin.getItemManager().createItem(itemId, amount);
                if (item != null) {
                    player.getInventory().addItem(item);
                }
            }
            case "CLOSE" -> {
                endDialogue(player);
            }
        }
    }

    /**
     * 조건 체크
     */
    public boolean checkCondition(Player player, String condition) {
        if (condition == null || condition.isEmpty())
            return true;

        boolean negate = condition.startsWith("!");
        String actualCondition = negate ? condition.substring(1) : condition;

        String[] parts = actualCondition.split(":", 2);
        String condType = parts[0];
        String condValue = parts.length > 1 ? parts[1] : "";

        boolean result = switch (condType) {
            case "quest", "mission" -> {
                var userData = plugin.getUserDataManager().getUserData(player);
                var mission = userData.getMission(condValue);
                yield mission != null && mission.isCompleted();
            }
            case "level" -> {
                String[] lvlParts = condValue.split(":");
                var jobType = com.dreamwork.job.JobType.fromConfigKey(lvlParts[0]);
                int reqLevel = Integer.parseInt(lvlParts[1]);
                var userData = plugin.getUserDataManager().getUserData(player);
                yield userData.getJobLevel(jobType) >= reqLevel;
            }
            default -> true;
        };

        return negate != result;
    }

    /**
     * 대화 세션 가져오기
     */
    public DialogueSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    /**
     * 대화 스크립트 존재 여부
     */
    public boolean hasDialogue(String npcId) {
        return dialogues.containsKey(npcId);
    }

    /**
     * 기본 대화 파일 생성
     */
    private void createDefaultDialogues(File folder) {
        // blacksmith.yml 생성은 별도로 처리
        plugin.saveResource("dialogues/blacksmith.yml", false);
    }

    /**
     * 리로드
     */
    public void reload() {
        activeSessions.clear();
        loadDialogues();
    }
}
