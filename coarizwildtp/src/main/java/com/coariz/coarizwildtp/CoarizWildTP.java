package com.coariz.coarizwildtp;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoarizWildTP extends JavaPlugin {

    private Economy econ = null;
    boolean economyEnabled = false; // Package-private
    private FileConfiguration langConfig = null;
    private File langFile = null;
    public ConcurrentHashMap<UUID, Long> cooldownMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<UUID, Location> initialLocationMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<UUID, BukkitRunnable> teleportTasks = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("Loading CoarizWildTP...");

        saveDefaultConfig();
        saveLangFile();

        if (setupEconomy()) { // setupEconomy is public
            economyEnabled = true;
        } else {
            getLogger().warning("Vault plugin not found or no economy provider found! Economy features will be disabled.");
        }

        // Register commands
        WildTeleportCommand wildTeleportCommand = new WildTeleportCommand(this);

        if (getCommand("wild") != null) {
            getCommand("wild").setExecutor(wildTeleportCommand);
        } else {
            getLogger().severe("Command 'wild' is not defined in plugin.yml!");
        }

        if (getCommand("rtp") != null) {
            getCommand("rtp").setExecutor(wildTeleportCommand);
        } else {
            getLogger().severe("Command 'rtp' is not defined in plugin.yml!");
        }

        if (getCommand("rtpadmin") != null) {
            getCommand("rtpadmin").setExecutor(new RTPAdminCommand(this, wildTeleportCommand));
        } else {
            getLogger().severe("Command 'rtpadmin' is not defined in plugin.yml!");
        }

        if (getCommand("coarizwildtp") != null) {
            getCommand("coarizwildtp").setExecutor(new CoarizWildTPCommand(this));
        } else {
            getLogger().severe("Command 'coarizwildtp' is not defined in plugin.yml!");
        }

        getLogger().info("CoarizWildTP has been enabled!");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        reloadLangFile();
        getLogger().info("Configuration reloaded.");
    }

    private void saveLangFile() {
        if (langFile == null) {
            langFile = new File(getDataFolder(), "lang.yml");
        }
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }
    }

    private void reloadLangFile() {
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Save default lang.yml if it doesn't exist
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
            langConfig = YamlConfiguration.loadConfiguration(langFile);
        }
    }

    public FileConfiguration getLangConfig() {
        if (langConfig == null) {
            reloadLangFile();
        }
        return langConfig;
    }

    public FileConfiguration getConfig() {
        return super.getConfig();
    }

    public Economy getEconomy() {
        return econ;
    }

    public boolean isEconomyEnabled() {
        return economyEnabled;
    }

    public String colorize(String message) {
        if (message == null) return "";

        // Regular expression to match hex color codes
        Pattern hexPattern = Pattern.compile("<#([A-Fa-f0-9]{6})>");
        Matcher matcher = hexPattern.matcher(message);

        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "§x§" + matcher.group(1).charAt(0) + "§" + matcher.group(1).charAt(1) + "§" + matcher.group(1).charAt(2) + "§" + matcher.group(1).charAt(3) + "§" + matcher.group(1).charAt(4) + "§" + matcher.group(1).charAt(5));
        }
        matcher.appendTail(buffer);

        return buffer.toString().replace("&", "§");
    }

    public boolean setupEconomy() { // Made public
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault plugin not found!");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("No economy provider found!");
            return false;
        }

        econ = rsp.getProvider();
        return econ != null;
    }

    public ConcurrentHashMap<UUID, Location> getInitialLocationMap() {
        return initialLocationMap;
    }

    public ConcurrentHashMap<UUID, BukkitRunnable> getTeleportTasks() {
        return teleportTasks;
    }
}