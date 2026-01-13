package com.dreamwork.core;

import com.dreamwork.DreamWorkPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 설정 파일 관리자
 * 
 * 모든 YAML 설정 파일을 로드하고 관리하는 클래스입니다.
 * 폴더 구조 자동 생성 및 기본 파일 복사 기능을 제공합니다.
 * 
 * @author DreamWork Team
 */
public class ConfigManager {

    private final DreamWorkPlugin plugin;

    // 설정 파일 캐시
    private FileConfiguration mainConfig;
    private FileConfiguration messagesConfig;

    // 직업별 설정 캐시
    private final Map<String, FileConfiguration> jobConfigs = new HashMap<>();

    // 아이템 설정 캐시
    private final Map<String, FileConfiguration> itemConfigs = new HashMap<>();

    // 미션 설정 캐시
    private final Map<String, FileConfiguration> missionConfigs = new HashMap<>();

    // GUI 설정 캐시
    private final Map<String, FileConfiguration> guiConfigs = new HashMap<>();

    public ConfigManager(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 모든 설정 파일 로드
     */
    public void loadAllConfigs() {
        // 폴더 구조 생성
        createDirectories();

        // 기본 설정 파일 생성 및 로드
        saveDefaultConfigs();

        // 메인 설정 로드
        mainConfig = loadConfig("config.yml");
        messagesConfig = loadConfig("messages.yml");

        // 하위 폴더 설정 파일 로드
        loadConfigsFromDirectory("jobs", jobConfigs);
        loadConfigsFromDirectory("items", itemConfigs);
        loadConfigsFromDirectory("missions", missionConfigs);
        loadConfigsFromDirectory("gui", guiConfigs);

        plugin.log(Level.INFO, "설정 파일 로드 완료!");
    }

    /**
     * 필요한 폴더 구조 생성
     */
    private void createDirectories() {
        String[] directories = { "jobs", "items", "missions", "gui", "database" };

        for (String dir : directories) {
            File folder = new File(plugin.getDataFolder(), dir);
            if (!folder.exists()) {
                folder.mkdirs();
                plugin.debug("폴더 생성됨: " + dir);
            }
        }
    }

    /**
     * 기본 설정 파일을 plugins 폴더에 복사
     */
    private void saveDefaultConfigs() {
        // 메인 설정 파일
        plugin.saveDefaultConfig();
        saveResourceIfNotExists("messages.yml");

        // 직업 설정 파일
        saveResourceIfNotExists("jobs/miner.yml");
        saveResourceIfNotExists("jobs/farmer.yml");
        saveResourceIfNotExists("jobs/fisher.yml");
        saveResourceIfNotExists("jobs/hunter.yml");
        saveResourceIfNotExists("jobs/adventurer.yml");

        // 아이템 설정 파일
        saveResourceIfNotExists("items/minerals.yml");
        saveResourceIfNotExists("items/crops.yml");
        saveResourceIfNotExists("items/fishes.yml");
        saveResourceIfNotExists("items/tools.yml");

        // 미션 설정 파일
        saveResourceIfNotExists("missions/daily_missions.yml");
        saveResourceIfNotExists("missions/job_missions.yml");

        // GUI 설정 파일
        saveResourceIfNotExists("gui/main_dashboard.yml");
    }

    /**
     * 리소스 파일이 존재하지 않으면 복사
     */
    private void saveResourceIfNotExists(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            // 리소스가 JAR에 있으면 복사, 없으면 빈 파일 생성
            InputStream resource = plugin.getResource(resourcePath);
            if (resource != null) {
                plugin.saveResource(resourcePath, false);
                plugin.debug("기본 파일 생성됨: " + resourcePath);
            } else {
                // 리소스가 없으면 빈 YAML 파일 생성
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                    plugin.debug("빈 파일 생성됨: " + resourcePath);
                } catch (IOException e) {
                    plugin.log(Level.WARNING, "파일 생성 실패: " + resourcePath);
                }
            }
        }
    }

    /**
     * 특정 디렉토리의 모든 YAML 파일 로드
     */
    private void loadConfigsFromDirectory(String directory, Map<String, FileConfiguration> configMap) {
        configMap.clear();

        File folder = new File(plugin.getDataFolder(), directory);
        if (!folder.exists() || !folder.isDirectory()) {
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            String fileName = file.getName().replace(".yml", "");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            // 기본 설정 파일이 있으면 누락된 값 채우기
            InputStream defaultStream = plugin.getResource(directory + "/" + file.getName());
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                config.setDefaults(defaultConfig);
            }

            configMap.put(fileName, config);
            plugin.debug("설정 로드됨: " + directory + "/" + file.getName());
        }
    }

    /**
     * 단일 설정 파일 로드
     */
    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // 기본값 설정
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }

        return config;
    }

    /**
     * 설정 파일 저장
     */
    public void saveConfig(String fileName, FileConfiguration config) {
        File file = new File(plugin.getDataFolder(), fileName);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "설정 파일 저장 실패: " + fileName);
            e.printStackTrace();
        }
    }

    // ==================== Getter 메서드 ====================

    /**
     * 메인 설정 파일 (config.yml)
     */
    public FileConfiguration getConfig() {
        return mainConfig;
    }

    /**
     * 메시지 설정 파일 (messages.yml)
     */
    public FileConfiguration getMessages() {
        return messagesConfig;
    }

    /**
     * 직업 설정 파일
     * 
     * @param jobName 직업 이름 (예: miner, farmer)
     */
    public FileConfiguration getJobConfig(String jobName) {
        return jobConfigs.get(jobName.toLowerCase());
    }

    /**
     * 아이템 설정 파일
     * 
     * @param category 카테고리 (예: minerals, crops)
     */
    public FileConfiguration getItemConfig(String category) {
        return itemConfigs.get(category.toLowerCase());
    }

    /**
     * 미션 설정 파일
     * 
     * @param missionFile 파일 이름 (예: daily_missions)
     */
    public FileConfiguration getMissionConfig(String missionFile) {
        return missionConfigs.get(missionFile.toLowerCase());
    }

    /**
     * GUI 설정 파일
     * 
     * @param guiName GUI 이름 (예: main_dashboard)
     */
    public FileConfiguration getGuiConfig(String guiName) {
        return guiConfigs.get(guiName.toLowerCase());
    }

    /**
     * 모든 직업 설정 반환
     */
    public Map<String, FileConfiguration> getAllJobConfigs() {
        return new HashMap<>(jobConfigs);
    }

    /**
     * 모든 아이템 설정 반환
     */
    public Map<String, FileConfiguration> getAllItemConfigs() {
        return new HashMap<>(itemConfigs);
    }

    /**
     * 모든 미션 설정 반환
     */
    public Map<String, FileConfiguration> getAllMissionConfigs() {
        return new HashMap<>(missionConfigs);
    }

    /**
     * 모든 GUI 설정 반환
     */
    public Map<String, FileConfiguration> getAllGuiConfigs() {
        return new HashMap<>(guiConfigs);
    }

    // ==================== 헬퍼 메서드 ====================

    /**
     * 메시지 가져오기 (색상 코드 변환 포함)
     * 
     * @param path 메시지 경로
     * @return 포맷팅된 메시지
     */
    public String getMessage(String path) {
        String message = messagesConfig.getString(path, "&c메시지를 찾을 수 없음: " + path);
        return translateColors(message);
    }

    /**
     * 프리픽스가 붙은 메시지 가져오기
     */
    public String getPrefixedMessage(String path) {
        String prefix = getMessage("prefix");
        String message = getMessage(path);
        return prefix + message;
    }

    /**
     * 플레이스홀더 치환이 포함된 메시지 가져오기
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    /**
     * 색상 코드 변환 (&로 시작하는 코드를 §로 변환)
     */
    public String translateColors(String text) {
        if (text == null)
            return "";
        return text.replace("&", "§");
    }
}
