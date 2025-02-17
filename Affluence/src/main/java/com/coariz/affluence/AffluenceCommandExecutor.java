package com.coariz.affluence;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AffluenceCommandExecutor implements CommandExecutor {

    private final Affluence plugin;

    public AffluenceCommandExecutor(Affluence plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("affluence") || command.getName().equalsIgnoreCase("aff")) {
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }

            String subCommand = args[0].toLowerCase();

            if (subCommand.equalsIgnoreCase("help")) {
                sendHelp(sender);
                return true;
            }

            if (subCommand.equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("affluence.admin")) {
                    sender.sendMessage(plugin.formatMessage("&cYou don't have permission to do that!"));
                    return true;
                }
                plugin.reloadConfigurations();
                sender.sendMessage(plugin.formatMessage("&8[&dAffluence&8] &fPlugin successfully reloaded!"));
                return true;
            }

            if (subCommand.equalsIgnoreCase("set")) {
                if (!sender.hasPermission("affluence.admin")) {
                    sender.sendMessage(plugin.formatMessage("&cYou don't have permission to do that!"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.formatMessage("&cUsage: /aff set <path> <value>"));
                    return true;
                }
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

            if (subCommand.equalsIgnoreCase("balance") || subCommand.equalsIgnoreCase("bal")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.formatMessage("&cThis command can only be used by players!"));
                    return true;
                }
                if (!sender.hasPermission("affluence.use")) {
                    sender.sendMessage(plugin.formatMessage("&cYou don't have permission to do that!"));
                    return true;
                }
                Player player = (Player) sender;
                UUID playerId = player.getUniqueId();
                double balance = plugin.getPlayerBalance(playerId);
                player.sendMessage(plugin.formatMessage("&8[&dAffluence&8] &fYour balance is: $" + balance));
                return true;
            }

            if (subCommand.equalsIgnoreCase("deposit") || subCommand.equalsIgnoreCase("dep")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.formatMessage("&cThis command can only be used by players!"));
                    return true;
                }
                if (!sender.hasPermission("affluence.use")) {
                    sender.sendMessage(plugin.formatMessage("&cYou don't have permission to do that!"));
                    return true;
                }
                Player player = (Player) sender;
                UUID playerId = player.getUniqueId();
                if (args.length < 2) {
                    player.sendMessage(plugin.formatMessage("&cUsage: /aff dep <amount>"));
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[1]);
                    if (amount <= 0) {
                        player.sendMessage(plugin.formatMessage("&cAmount must be greater than 0!"));
                        return true;
                    }
                    double vaultBalance = plugin.getEconomy().getBalance(player);
                    if (vaultBalance < amount) {
                        player.sendMessage(plugin.formatMessage("&cYou do not have enough money in your Vault balance!"));
                        return true;
                    }
                    plugin.getEconomy().withdrawPlayer(player, amount);
                    plugin.depositPlayerBalance(playerId, amount);
                    player.sendMessage(plugin.formatMessage("&8[&dAffluence&8] &fDeposited $" + amount + " into your Affluence balance."));
                    return true;
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.formatMessage("&cInvalid amount! Please enter a valid number."));
                    return true;
                }
            }

            if (subCommand.equalsIgnoreCase("withdraw") || subCommand.equalsIgnoreCase("with")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.formatMessage("&cThis command can only be used by players!"));
                    return true;
                }
                if (!sender.hasPermission("affluence.use")) {
                    sender.sendMessage(plugin.formatMessage("&cYou don't have permission to do that!"));
                    return true;
                }
                Player player = (Player) sender;
                UUID playerId = player.getUniqueId();
                if (args.length < 2) {
                    player.sendMessage(plugin.formatMessage("&cUsage: /aff with <amount>"));
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[1]);
                    if (amount <= 0) {
                        player.sendMessage(plugin.formatMessage("&cAmount must be greater than 0!"));
                        return true;
                    }
                    double affluenceBalance = plugin.getPlayerBalance(playerId);
                    if (affluenceBalance < amount) {
                        player.sendMessage(plugin.formatMessage("&cYou do not have enough money in your Affluence balance!"));
                        return true;
                    }
                    plugin.withdrawPlayerBalance(playerId, amount);
                    plugin.getEconomy().depositPlayer(player, amount);
                    player.sendMessage(plugin.formatMessage("&8[&dAffluence&8] &fWithdrew $" + amount + " from your Affluence balance."));
                    return true;
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.formatMessage("&cInvalid amount! Please enter a valid number."));
                    return true;
                }
            }

            sender.sendMessage(plugin.formatMessage("&cInvalid subcommand! Use /aff help for more information."));
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
        sender.sendMessage(plugin.formatMessage("&8[&dAffluence&8] &7List of commands, available to all players"));
        sender.sendMessage(plugin.formatMessage("&7/aff balance or /aff bal"));
        sender.sendMessage(plugin.formatMessage("&7/aff deposit or /aff dep <amount>"));
        sender.sendMessage(plugin.formatMessage("&7/aff withdraw or /aff with <amount>"));
        sender.sendMessage(plugin.formatMessage("- end -"));
    }
}