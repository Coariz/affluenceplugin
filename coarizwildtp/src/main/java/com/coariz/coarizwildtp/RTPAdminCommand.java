package com.coariz.coarizwildtp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class RTPAdminCommand implements CommandExecutor {

    private final CoarizWildTP plugin;
    private final WildTeleportCommand wildTeleportCommand;

    public RTPAdminCommand(CoarizWildTP plugin, WildTeleportCommand wildTeleportCommand) {
        this.plugin = plugin;
        this.wildTeleportCommand = wildTeleportCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("coarizwildtp.admin")) {
            sender.sendMessage(plugin.colorize(plugin.getLangConfig().getString("no_permission", "You do not have permission to use this command.")));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(plugin.colorize(plugin.getLangConfig().getString("usage_rtpadmin", "Usage: /rtpadmin <player>")));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(plugin.colorize(plugin.getLangConfig().getString("player_not_found", "Player %player% not found.").replace("%player%", args[0])));
            return true;
        }

        FileConfiguration config = plugin.getConfig();
        FileConfiguration langConfig = plugin.getLangConfig();

        // Teleport the player immediately without cooldown or delay
        wildTeleportCommand.teleportPlayer(targetPlayer, config, langConfig, true);

        return true;
    }
}