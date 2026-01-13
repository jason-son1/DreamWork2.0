package com.dreamwork.hook;

import com.dreamwork.DreamWorkPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

/**
 * Vault 경제 시스템 Hook
 * 
 * Vault API를 통해 서버의 경제 플러그인과 연동합니다.
 * 
 * @author DreamWork Team
 */
public class VaultHook {

    private final DreamWorkPlugin plugin;
    private Economy economy;
    private boolean enabled = false;

    public VaultHook(DreamWorkPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Vault 연동 설정
     * 
     * @return 성공 여부
     */
    public boolean setup() {
        // Vault 플러그인 확인
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.log(Level.WARNING, "Vault 플러그인을 찾을 수 없습니다!");
            return false;
        }

        // Economy 서비스 가져오기
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            plugin.log(Level.WARNING, "Economy 서비스를 찾을 수 없습니다! (EssentialsX 등 경제 플러그인 필요)");
            return false;
        }

        economy = rsp.getProvider();
        enabled = economy != null;

        if (enabled) {
            plugin.debug("Economy 연동 완료: " + economy.getName());
        }

        return enabled;
    }

    /**
     * 연동 상태 확인
     */
    public boolean isEnabled() {
        return enabled && economy != null;
    }

    /**
     * 플레이어 잔액 확인
     */
    public double getBalance(Player player) {
        if (!isEnabled())
            return 0;
        return economy.getBalance(player);
    }

    /**
     * 플레이어 잔액 확인 (오프라인)
     */
    public double getBalance(OfflinePlayer player) {
        if (!isEnabled())
            return 0;
        return economy.getBalance(player);
    }

    /**
     * 잔액 충분 여부 확인
     */
    public boolean has(Player player, double amount) {
        if (!isEnabled())
            return false;
        return economy.has(player, amount);
    }

    /**
     * 돈 입금
     * 
     * @return 성공 여부
     */
    public boolean deposit(Player player, double amount) {
        if (!isEnabled())
            return false;

        EconomyResponse response = economy.depositPlayer(player, amount);

        if (response.transactionSuccess()) {
            plugin.debug(player.getName() + "에게 " + amount + "D 입금");
            return true;
        } else {
            plugin.debug("입금 실패: " + response.errorMessage);
            return false;
        }
    }

    /**
     * 돈 입금 (오프라인)
     */
    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isEnabled())
            return false;

        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * 돈 출금
     * 
     * @return 성공 여부
     */
    public boolean withdraw(Player player, double amount) {
        if (!isEnabled())
            return false;

        // 잔액 확인
        if (!economy.has(player, amount)) {
            plugin.debug(player.getName() + " 잔액 부족 (필요: " + amount + ")");
            return false;
        }

        EconomyResponse response = economy.withdrawPlayer(player, amount);

        if (response.transactionSuccess()) {
            plugin.debug(player.getName() + "에게서 " + amount + "D 출금");
            return true;
        } else {
            plugin.debug("출금 실패: " + response.errorMessage);
            return false;
        }
    }

    /**
     * 돈 출금 (오프라인)
     */
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isEnabled())
            return false;

        if (!economy.has(player, amount)) {
            return false;
        }

        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * 화폐 단위 이름
     */
    public String getCurrencyName() {
        if (!isEnabled())
            return "D";
        return economy.currencyNamePlural();
    }

    /**
     * 화폐 단위 이름 (단수형)
     */
    public String getCurrencyNameSingular() {
        if (!isEnabled())
            return "D";
        return economy.currencyNameSingular();
    }

    /**
     * 포맷팅된 금액
     */
    public String format(double amount) {
        if (!isEnabled())
            return String.format("%.2f D", amount);
        return economy.format(amount);
    }

    /**
     * Economy 객체 직접 접근
     */
    public Economy getEconomy() {
        return economy;
    }
}
