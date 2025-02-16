package com.coariz.affluence;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;

import java.util.Calendar;
import java.util.Date;

public class Affluence extends JavaPlugin implements Listener {

    private Economy economy;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        if (!setupEconomy()) {
            getLogger().severe(formatMessage("&8[&dAffluence&8] &cVault not found! Disabling plugin..."));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);

        // Register the /affluence command
        getCommand("affluence").setExecutor(new AffluenceCommandExecutor(this));
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        double deathMoneyLoss = config.getDouble("death_money_loss");
        economy.withdrawPlayer(player, deathMoneyLoss);

        // Check if there is a killer
        if (event.getEntity().getKiller() instanceof Player) {
            Player killer = event.getEntity().getKiller();
            double moneyPerKill = config.getDouble("money_per_kill");
            economy.depositPlayer(killer, moneyPerKill);
            killer.sendMessage(formatMessage("&cYou earned $" + moneyPerKill + " for killing " + player.getName()));
        }

        // Check if the player's balance is below the negative money threshold
        double negativeMoneyThreshold = config.getDouble("negative_money_threshold");
        double playerBalance = economy.getBalance(player);
        if (playerBalance < negativeMoneyThreshold) {
            banPlayer(player);
        } else {
            applyBoosts(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyBoosts(player);
    }

    // Method to calculate boosts based on player's balance
    public void applyBoosts(Player player) {
        double balance = economy.getBalance(player);
        double healthBoostPerValue = config.getDouble("health_boost_per_value");
        double healthBoostValue = config.getDouble("health_boost_value");
        double damageBoostPerValue = config.getDouble("damage_boost_per_value");
        double damageBoostValue = config.getDouble("damage_boost_value");

        double healthBoost = (balance / healthBoostValue) * healthBoostPerValue;
        double damageBoost = (balance / damageBoostValue) * damageBoostPerValue;

        // Apply health boost
        player.setHealthScale(20 + healthBoost);

        // Apply damage boost
        // Note: Bukkit API does not provide a direct way to modify player damage boost,
        // but you can handle it through custom events or other means.
        // For demonstration, let's print the damage boost to the console.
        getLogger().info(formatMessage("&cPlayer " + player.getName() + " has a damage boost of: " + damageBoost));
    }

    // Method to ban a player
    public void banPlayer(Player player) {
        if (!config.getBoolean("ban_players")) {
            return;
        }

        double negativeMoneyThreshold = config.getDouble("negative_money_threshold");
        String banDuration = config.getString("ban_player_for");
        Date unbanDate = parseBanDuration(banDuration);

        player.kickPlayer(formatMessage("&cYou have been temporarily banned for reaching a balance of " + negativeMoneyThreshold + " or less."));
        Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), formatMessage("Reached a balance of " + negativeMoneyThreshold + " or less."), unbanDate, null);
        economy.depositPlayer(player, -economy.getBalance(player)); // Set balance to 0
        getLogger().info(formatMessage("&cPlayer " + player.getName() + " has been temporarily banned for reaching a balance of " + negativeMoneyThreshold + " or less."));
    }

    // Method to parse ban duration
    private Date parseBanDuration(String duration) {
        Calendar calendar = Calendar.getInstance();
        int amount = Integer.parseInt(duration.replaceAll("[^0-9]", ""));
        char unit = duration.charAt(duration.length() - 1);

        switch (unit) {
            case 'h':
                calendar.add(Calendar.HOUR_OF_DAY, amount);
                break;
            case 'd':
                calendar.add(Calendar.DAY_OF_MONTH, amount);
                break;
            case 'y':
                calendar.add(Calendar.YEAR, amount);
                break;
            default:
                calendar.add(Calendar.DAY_OF_MONTH, 30); // Default to 30 days if unknown unit
                break;
        }

        return calendar.getTime();
    }

    // Method to reload the configuration
    public void reloadConfigurations() {
        reloadConfig();
        config = getConfig();
        getLogger().info(formatMessage("&8[&dAffluence&8] &fPlugin successfully reloaded!"));
    }

    // Method to set a configuration value
    public void setConfigValue(String path, Object value) {
        config.set(path, value);
        saveConfig();
        getLogger().info(formatMessage("&8[&dAffluence&8] &fConfig value '" + path + "' set to " + value));
    }

    // Utility method to format messages with Minecraft color codes
    public String formatMessage(String message) {
        return message.replace("&", "§");
    }

    // Getter for config
    public FileConfiguration getConfigurations() {
        return config;
    }

    // Getter for economy
    public Economy getEconomy() {
        return economy;
    }
}