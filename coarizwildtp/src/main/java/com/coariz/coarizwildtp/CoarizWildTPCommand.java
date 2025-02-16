package com.coariz.coarizwildtp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CoarizWildTPCommand implements CommandExecutor {

    private final CoarizWildTP plugin;

    public CoarizWildTPCommand(CoarizWildTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("coarizwildtp.reload")) {
            sender.sendMessage(plugin.colorize(plugin.getLangConfig().getString("no_permission", "You do not have permission to use this command.")));
            return true;
        }

        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(plugin.colorize(plugin.getLangConfig().getString("usage_reload", "Usage: /coarizwildtp reload")));
            return true;
        }

        try {
            plugin.reloadConfig();
            sender.sendMessage(plugin.colorize(plugin.getLangConfig().getString("reload_success", "CoarizWildTP configuration reloaded.")));

            // Optionally, reinitialize economy if needed
            if (plugin.setupEconomy()) {
                plugin.setEconomyEnabled(true);
                sender.sendMessage(plugin.colorize(plugin.getLangConfig().getString("economy_reenabled", "Economy features re-enabled.")));
            } else {
                plugin.setEconomyEnabled(false);
                sender.sendMessage(plugin.colorize(plugin.getLangConfig().getString("economy_disabled", "Vault or an economy plugin not found! Economy features are disabled.")));
            }
        } catch (Exception e) {
            sender.sendMessage(plugin.colorize(plugin.getLangConfig().getString("reload_failure", "Failed to reload CoarizWildTP configuration.")));
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error reloading configuration:", e);
        }

        return true;
    }
}