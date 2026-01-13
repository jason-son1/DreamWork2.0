package com.dreamwork;

import com.dreamwork.command.DreamWorkCommand;
import com.dreamwork.core.ConfigManager;
import com.dreamwork.core.UserDataManager;
import com.dreamwork.gui.GuiListener;
import com.dreamwork.gui.GuiManager;
import com.dreamwork.hook.*;
import com.dreamwork.item.ItemManager;
import com.dreamwork.job.JobManager;
import com.dreamwork.job.listener.*;
import com.dreamwork.mission.MissionManager;
import com.dreamwork.skill.SkillManager;
import com.dreamwork.skill.impl.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * DreamWork 플러그인 메인 클래스
 * 
 * 모든 매니저와 리스너를 초기화하고 관리하는 핵심 클래스입니다.
 * 
 * @author DreamWork Team
 * @version 2.0.0
 */
public class DreamWorkPlugin extends JavaPlugin {

    // 싱글톤 인스턴스
    private static DreamWorkPlugin instance;

    // 핵심 매니저
    private ConfigManager configManager;
    private UserDataManager userDataManager;
    private ItemManager itemManager;
    private JobManager jobManager;
    private GuiManager guiManager;
    private MissionManager missionManager;
    private SkillManager skillManager;

    // 외부 플러그인 Hook
    private VaultHook vaultHook;
    private TownyHook townyHook;
    private LuckPermsHook luckPermsHook;
    private CitizensHook citizensHook;

    // 스킬 및 리스너 참조 (상호 연결용)
    private MinerListener minerListener;
    private com.dreamwork.skill.impl.MinersTrance minersTrance;

    // 디버그 모드
    private boolean debugMode = false;

    @Override
    public void onEnable() {
        // 시작 시간 기록 (성능 측정)
        long startTime = System.currentTimeMillis();

        // 싱글톤 인스턴스 설정
        instance = this;

        // 로고 출력
        printLogo();

        try {
            // 1. 설정 파일 로드
            log(Level.INFO, "설정 파일을 로드하는 중...");
            configManager = new ConfigManager(this);
            configManager.loadAllConfigs();
            debugMode = configManager.getConfig().getBoolean("general.debug", false);

            // 2. 외부 플러그인 Hook 초기화
            log(Level.INFO, "외부 플러그인 연동을 설정하는 중...");
            setupHooks();

            // 3. 유저 데이터 매니저 초기화
            log(Level.INFO, "데이터베이스를 초기화하는 중...");
            userDataManager = new UserDataManager(this);
            userDataManager.initialize();

            // 4. 아이템 매니저 초기화
            log(Level.INFO, "아이템 시스템을 초기화하는 중...");
            itemManager = new ItemManager(this);
            itemManager.loadItems();

            // 5. 직업 매니저 초기화
            log(Level.INFO, "직업 시스템을 초기화하는 중...");
            jobManager = new JobManager(this);

            // 6. GUI 매니저 초기화
            log(Level.INFO, "GUI 시스템을 초기화하는 중...");
            guiManager = new GuiManager(this);

            // 7. 미션 매니저 초기화
            log(Level.INFO, "미션 시스템을 초기화하는 중...");
            missionManager = new MissionManager(this);
            missionManager.loadMissions();

            // 8. 스킬 매니저 초기화
            log(Level.INFO, "스킬 시스템을 초기화하는 중...");
            skillManager = new SkillManager(this);

            // 8. 이벤트 리스너 등록
            log(Level.INFO, "이벤트 리스너를 등록하는 중...");
            registerListeners();

            // 9. 명령어 등록
            log(Level.INFO, "명령어를 등록하는 중...");
            registerCommands();

            // 10. PlaceholderAPI 확장 등록
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new DreamWorkExpansion(this).register();
                log(Level.INFO, "PlaceholderAPI 확장이 등록되었습니다.");
            }

            // 완료 메시지
            long endTime = System.currentTimeMillis();
            log(Level.INFO, "===========================================");
            log(Level.INFO, "DreamWork v" + getDescription().getVersion() + " 활성화 완료!");
            log(Level.INFO, "로딩 시간: " + (endTime - startTime) + "ms");
            log(Level.INFO, "===========================================");

        } catch (Exception e) {
            log(Level.SEVERE, "플러그인 초기화 중 오류가 발생했습니다!");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        log(Level.INFO, "DreamWork 플러그인을 비활성화하는 중...");

        // 모든 플레이어 데이터 저장
        if (userDataManager != null) {
            userDataManager.saveAllData();
            userDataManager.shutdown();
        }

        // 싱글톤 인스턴스 해제
        instance = null;

        log(Level.INFO, "DreamWork 플러그인이 비활성화되었습니다.");
    }

    /**
     * 외부 플러그인 Hook 설정
     */
    private void setupHooks() {
        // Vault 연동 (필수)
        vaultHook = new VaultHook(this);
        if (!vaultHook.setup()) {
            log(Level.SEVERE, "Vault 연동에 실패했습니다! 경제 시스템이 작동하지 않습니다.");
        } else {
            log(Level.INFO, "Vault 연동 성공!");
        }

        // Towny 연동 (선택)
        if (configManager.getConfig().getBoolean("hooks.towny-enabled", true)) {
            townyHook = new TownyHook(this);
            if (townyHook.isEnabled()) {
                log(Level.INFO, "Towny 연동 성공!");
            }
        }

        // LuckPerms 연동 (선택)
        if (configManager.getConfig().getBoolean("hooks.luckperms-enabled", true)) {
            luckPermsHook = new LuckPermsHook(this);
            if (luckPermsHook.isEnabled()) {
                log(Level.INFO, "LuckPerms 연동 성공!");
            }
        }

        // Citizens 연동 (선택)
        if (configManager.getConfig().getBoolean("hooks.citizens-enabled", true)) {
            citizensHook = new CitizensHook(this);
            if (citizensHook.isEnabled()) {
                log(Level.INFO, "Citizens 연동 성공!");
            }
        }
    }

    /**
     * 이벤트 리스너 등록
     */
    private void registerListeners() {
        // GUI 리스너
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);

        // 직업별 리스너
        Bukkit.getPluginManager().registerEvents(new MinerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new FarmerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new FisherListener(this), this);
        Bukkit.getPluginManager().registerEvents(new HunterListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AdventurerListener(this), this);

        // 유저 데이터 이벤트
        Bukkit.getPluginManager().registerEvents(userDataManager, this);

        // 미션 리스너
        Bukkit.getPluginManager().registerEvents(new com.dreamwork.mission.MissionListener(this), this);

        // 스킬 리스너 (패시브 스킬)
        Bukkit.getPluginManager().registerEvents(new GreenThumb(this), this);
        Bukkit.getPluginManager().registerEvents(new CriticalEye(this), this);
        Bukkit.getPluginManager().registerEvents(new Sensitivity(this), this);

        // 광부 스킬
        Bukkit.getPluginManager().registerEvents(new CaveAdaptation(this), this);
        Bukkit.getPluginManager().registerEvents(new MinersTrance(this), this);

        // 탐험가 스킬
        Bukkit.getPluginManager().registerEvents(new ExplorerSense(this), this);
    }

    /**
     * 명령어 등록
     */
    private void registerCommands() {
        DreamWorkCommand mainCommand = new DreamWorkCommand(this);
        getCommand("dreamwork").setExecutor(mainCommand);
        getCommand("dreamwork").setTabCompleter(mainCommand);
        getCommand("dwadmin").setExecutor(mainCommand);
        getCommand("dwadmin").setTabCompleter(mainCommand);
    }

    /**
     * 플러그인 로고 출력
     */
    private void printLogo() {
        getLogger().info("");
        getLogger().info("  ____                        __        __         _    ");
        getLogger().info(" |  _ \\ _ __ ___  __ _ _ __ __\\ \\      / /__  _ __| | __");
        getLogger().info(" | | | | '__/ _ \\/ _` | '_ ` _ \\ \\ /\\ / / _ \\| '__| |/ /");
        getLogger().info(" | |_| | | |  __/ (_| | | | | | \\ V  V / (_) | |  |   < ");
        getLogger().info(" |____/|_|  \\___|\\__,_|_| |_| |_|\\_/\\_/ \\___/|_|  |_|\\_\\");
        getLogger().info("");
        getLogger().info(" Version: " + getDescription().getVersion());
        getLogger().info("");
    }

    /**
     * 설정 파일 리로드
     */
    public void reload() {
        configManager.loadAllConfigs();
        debugMode = configManager.getConfig().getBoolean("general.debug", false);
        itemManager.loadItems();
        missionManager.loadMissions();
        guiManager.reload();
    }

    /**
     * 로그 출력 헬퍼 메서드
     */
    public void log(Level level, String message) {
        getLogger().log(level, message);
    }

    /**
     * 디버그 로그 출력
     */
    public void debug(String message) {
        if (debugMode) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    // ==================== Getter 메서드 ====================

    public static DreamWorkPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public UserDataManager getUserDataManager() {
        return userDataManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public MissionManager getMissionManager() {
        return missionManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public TownyHook getTownyHook() {
        return townyHook;
    }

    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }

    public CitizensHook getCitizensHook() {
        return citizensHook;
    }

    public boolean isDebugMode() {
        return debugMode;
    }
}
