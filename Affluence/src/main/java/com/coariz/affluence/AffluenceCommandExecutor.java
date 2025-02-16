package com.coariz.affluence;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AffluenceCommandExecutor implements CommandExecutor {

    private final Affluence plugin;

    public AffluenceCommandExecutor(Affluence plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("affluence")) {
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }

            String subCommand = args[0].toLowerCase();

            if (subCommand.equalsIgnoreCase("help")) {
                sendHelp(sender);
                return true;
            }

            if (!sender.hasPermission("affluence.admin")) {
                sender.sendMessage(plugin.formatMessage("&cYou don't have permission to do that!"));
                return true;
            }

            if (subCommand.equalsIgnoreCase("reload")) {
                plugin.reloadConfigurations();
                sender.sendMessage(plugin.formatMessage("&8[&dAffluence&8] &fPlugin successfully reloaded!"));
                return true;
            }

            if (subCommand.equalsIgnoreCase("set") && args.length >= 3) {
                String path = args[1];
                String valueStr = args[2];

                try {
                    Object value;
                    switch (path) {
                        case "health_boost_per_value":
                        case "health_boost_value":
                        case "damage_boost_per_value":
                        case "damage_boost_value":
                        case "death_money_loss":
                        case "money_per_kill":
                        case "negative_money_threshold":
                            value = Double.parseDouble(valueStr);
                            break;
                        case "ban_players":
                            value = Boolean.parseBoolean(valueStr);
                            break;
                        case "ban_player_for":
                            value = valueStr;
                            break;
                        default:
                            sender.sendMessage(plugin.formatMessage("&cInvalid configuration path!"));
                            return true;
                    }
                    plugin.setConfigValue(path, value);
                    sender.sendMessage(plugin.formatMessage("&8[&dAffluence&8] &fConfig value '" + path + "' set to " + value));
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.formatMessage("&cInvalid value! Please enter a valid number."));
                    return true;
                }
            }

            sender.sendMessage(plugin.formatMessage("&cInvalid subcommand! Use /affluence help for more information."));
            return true;
        }
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.formatMessage("&dAffluence plugin by Coariz"));
        sender.sendMessage(plugin.formatMessage("&8[&dAffluence&8] &7List of commands, requires &daffluence.admin &7permission to execute"));
        sender.sendMessage(plugin.formatMessage("&7/affluence reload"));
        sender.sendMessage(plugin.formatMessage("&7/affluence set health_boost_per_value <value>"));
        sender.sendMessage(plugin.formatMessage("&7/affluence set health_boost_value <value>"));
        sender.sendMessage(plugin.formatMessage("&7/affluence set damage_boost_per_value <value>"));
        sender.sendMessage(plugin.formatMessage("&7/affluence set damage_boost_value <value>"));
        sender.sendMessage(plugin.formatMessage("&7/affluence set death_money_loss <value>"));
        sender.sendMessage(plugin.formatMessage("&7/affluence set money_per_kill <value>"));
        sender.sendMessage(plugin.formatMessage("&7/affluence set negative_money_threshold <value>"));
        sender.sendMessage(plugin.formatMessage("&7/affluence set ban_players <true/false>"));
        sender.sendMessage(plugin.formatMessage("&7/affluence set ban_player_for <duration> (e.g., 30d, 1y, 10h)"));
        sender.sendMessage(plugin.formatMessage("- end -"));
    }
}