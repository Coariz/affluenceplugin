package com.coariz.coarizwildtp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WildTeleportCommand implements CommandExecutor, Listener {

    private final CoarizWildTP plugin;

    public WildTeleportCommand(CoarizWildTP plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.colorize(plugin.getLangConfig().getString("only_players", "Only players can use this command.")));
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = plugin.getConfig();
        FileConfiguration langConfig = plugin.getLangConfig();

        // Check if player has bypass cooldown permission
        if (player.hasPermission("coarizwildtp.bypasscooldown")) {
            teleportPlayer(player, config, langConfig, true); // Immediate teleportation
            return true;
        }

        // Check cooldown
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        if (plugin.getTeleportTasks().containsKey(playerId)) {
            player.sendMessage(plugin.colorize(langConfig.getString("already_in_progress", "You already have a teleportation in progress.")));
            return true;
        }

        if (plugin.cooldownMap.containsKey(playerId)) {
            long lastUsed = plugin.cooldownMap.get(playerId);
            long cooldownTime = config.getLong("cooldown", 5) * 1000L; // Convert seconds to milliseconds
            if (currentTime - lastUsed < cooldownTime) {
                long remainingTime = (lastUsed + cooldownTime - currentTime) / 1000L;
                player.sendMessage(plugin.colorize(langConfig.getString("cooldown_message", "You must wait %seconds% more seconds before using this command again.").replace("%seconds%", String.valueOf(remainingTime))));
                return true;
            }
        }

        teleportPlayer(player, config, langConfig, false); // Delayed teleportation

        return true;
    }

    public void teleportPlayer(Player player, FileConfiguration config, FileConfiguration langConfig, boolean immediate) {
        World world = Bukkit.getWorld(config.getString("world", player.getWorld().getName()));
        if (world == null) {
            player.sendMessage(plugin.colorize(langConfig.getString("invalid_world", "Invalid world specified in config.")));
            return;
        }

        Location safeLocation = findSafeLocation(world);
        if (safeLocation == null) {
            player.sendMessage(plugin.colorize(langConfig.getString("no_safe_location", "Could not find a safe location.")));
            return;
        }

        UUID playerId = player.getUniqueId();

        if (immediate) {
            // Teleport the player immediately
            player.teleport(safeLocation);
            player.sendMessage(plugin.colorize(langConfig.getString("teleport_success", "Teleported to a random location!")));

            // Apply invincibility
            int invincibilityDuration = config.getInt("invincibility_duration", 0); // Duration in seconds
            if (invincibilityDuration > 0) {
                player.setInvulnerable(true);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.setInvulnerable(false);
                    }
                }.runTaskLater(plugin, invincibilityDuration * 20L); // Convert seconds to ticks
            }

            // Apply effects
            boolean applyEffects = config.getBoolean("apply_effects", true);
            if (applyEffects) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 2, false, false)); // Blindness Level 3 (duration 3 seconds)
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 3, false, false)); // Slowness Level 4 (duration 4 seconds)
            }

            // Send title and subtitle
            boolean sendTitle = config.getBoolean("send_title", true);
            if (sendTitle) {
                String titleMessage = plugin.colorize(langConfig.getString("title_message", "Teleported"));
                String subtitleMessage = plugin.colorize(langConfig.getString("subtitle_message", "Coordinates: %x%, %y%, %z%")
                    .replace("%x%", String.valueOf(safeLocation.getBlockX()))
                    .replace("%y%", String.valueOf(safeLocation.getBlockY()))
                    .replace("%z%", String.valueOf(safeLocation.getBlockZ())));
                player.sendTitle(titleMessage, subtitleMessage, 10, 70, 20); // Fade in, stay, fade out in ticks
            }

            // Store cooldown
            plugin.cooldownMap.put(playerId, System.currentTimeMillis());

            // Remove initial location and teleport task
            plugin.getInitialLocationMap().remove(playerId);
            plugin.getTeleportTasks().remove(playerId);
        } else {
            int teleportDelay = config.getInt("teleport_delay", 5); // Default delay is 5 seconds
            boolean allowMovementDuringDelay = config.getBoolean("allow_movement_during_delay", false);

            player.sendMessage(plugin.colorize(langConfig.getString("teleport_delay_message", "Teleporting in %seconds% seconds...").replace("%seconds%", String.valueOf(teleportDelay))));

            // Store the player's initial location
            plugin.getInitialLocationMap().put(playerId, player.getLocation());

            // Create a BukkitRunnable task for teleportation
            BukkitRunnable teleportTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!allowMovementDuringDelay) {
                        Location initialLocation = plugin.getInitialLocationMap().get(playerId);
                        if (initialLocation != null && player.getWorld().equals(initialLocation.getWorld())) {
                            if (player.getLocation().distanceSquared(initialLocation) > 0.1) {
                                player.sendMessage(plugin.colorize(langConfig.getString("moved_during_delay", "You moved during the teleportation delay. Teleportation cancelled.")));
                                plugin.getInitialLocationMap().remove(playerId);
                                plugin.cooldownMap.remove(playerId);
                                plugin.getTeleportTasks().remove(playerId);
                                return;
                            }
                        }
                    }

                    // Teleport the player
                    player.teleport(safeLocation);
                    player.sendMessage(plugin.colorize(langConfig.getString("teleport_success", "Teleported to a random location!")));

                    // Apply invincibility
                    int invincibilityDuration = config.getInt("invincibility_duration", 0); // Duration in seconds
                    if (invincibilityDuration > 0) {
                        player.setInvulnerable(true);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.setInvulnerable(false);
                            }
                        }.runTaskLater(plugin, invincibilityDuration * 20L); // Convert seconds to ticks
                    }

                    // Apply effects
                    boolean applyEffects = config.getBoolean("apply_effects", true);
                    if (applyEffects) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 2, false, false)); // Blindness Level 3 (duration 3 seconds)
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 3, false, false)); // Slowness Level 4 (duration 4 seconds)
                    }

                    // Send title and subtitle
                    boolean sendTitle = config.getBoolean("send_title", true);
                    if (sendTitle) {
                        String titleMessage = plugin.colorize(langConfig.getString("title_message", "Teleported"));
                        String subtitleMessage = plugin.colorize(langConfig.getString("subtitle_message", "Coordinates: %x%, %y%, %z%")
                            .replace("%x%", String.valueOf(safeLocation.getBlockX()))
                            .replace("%y%", String.valueOf(safeLocation.getBlockY()))
                            .replace("%z%", String.valueOf(safeLocation.getBlockZ())));
                        player.sendTitle(titleMessage, subtitleMessage, 10, 70, 20); // Fade in, stay, fade out in ticks
                    }

                    // Store cooldown
                    plugin.cooldownMap.put(playerId, System.currentTimeMillis());

                    // Remove initial location and teleport task
                    plugin.getInitialLocationMap().remove(playerId);
                    plugin.getTeleportTasks().remove(playerId);
                }
            };

            // Schedule the teleportation with a delay
            teleportTask.runTaskLater(plugin, teleportDelay * 20L); // Convert seconds to ticks (1 tick = 0.05 seconds)

            // Store the teleport task in the map
            plugin.getTeleportTasks().put(playerId, teleportTask);
        }
    }

    private Location findSafeLocation(World world) {
        Set<Material> blacklist = new HashSet<>();
        for (String materialName : plugin.getConfig().getStringList("blacklisted_blocks")) {
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
                blacklist.add(material);
            }
        }

        Set<Material> allowedSurfaceMaterials = new HashSet<>();
        for (String materialName : plugin.getConfig().getStringList("allowed_surface_materials")) {
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
                allowedSurfaceMaterials.add(material);
            }
        }

        WorldBorder worldBorder = world.getWorldBorder();
        Location spawnLocation = world.getSpawnLocation();
        double spawnRadius = plugin.getConfig().getDouble("spawn_radius", 1000); // Default radius around spawn point

        int centerX = plugin.getConfig().getInt("center_x", 0);
        int centerZ = plugin.getConfig().getInt("center_z", 0);
        int radius = plugin.getConfig().getInt("radius", 2000);

        int maxAttempts = plugin.getConfig().getInt("max_attempts", 500); // Increased number of attempts
        boolean loggingEnabled = plugin.getConfig().getBoolean("logging_enabled", true);

        for (int i = 0; i < maxAttempts; i++) { // Try up to maxAttempts times
            int x = centerX + (int) (Math.random() * (2 * radius + 1)) - radius;
            int z = centerZ + (int) (Math.random() * (2 * radius + 1)) - radius;

            // Check if the location is within the world border
            if (!worldBorder.isInside(new Location(world, x, 0, z))) {
                if (loggingEnabled) {
                    plugin.getLogger().info("Attempt " + (i + 1) + ": Location (" + x + ", 0, " + z + ") is outside the world border.");
                }
                continue; // Continue to the next iteration of the loop
            }

            // Check if the location is too close to the spawn point
            Location loc = new Location(world, x, 0, z);
            if (loc.distance(spawnLocation) < spawnRadius) {
                if (loggingEnabled) {
                    plugin.getLogger().info("Attempt " + (i + 1) + ": Location (" + x + ", 0, " + z + ") is too close to the spawn point.");
                }
                continue; // Continue to the next iteration of the loop
            }

            int y = world.getHighestBlockYAt(x, z);
            Location safeLoc = new Location(world, x + 0.5, y + 1, z + 0.5);

            if (isSafeSurfaceLocation(safeLoc, blacklist, allowedSurfaceMaterials)) {
                if (loggingEnabled) {
                    plugin.getLogger().info("Attempt " + (i + 1) + ": Found safe surface location: " + safeLoc);
                }
                return safeLoc;
            } else {
                if (loggingEnabled) {
                    Material below = world.getBlockAt(x, y - 1, z).getType();
                    plugin.getLogger().info("Attempt " + (i + 1) + ": Location (" + x + ", " + y + ", " + z + ") is not a safe surface location.");
                    plugin.getLogger().info("Block below: " + below);
                }
            }
        }

        if (loggingEnabled) {
            plugin.getLogger().info("Failed to find a safe surface location after " + maxAttempts + " attempts.");
        }
        return null;
    }

    private boolean isSafeSurfaceLocation(Location loc, Set<Material> blacklist, Set<Material> allowedSurfaceMaterials) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        World world = loc.getWorld();

        Material below = world.getBlockAt(x, y - 1, z).getType();

        if (below.isSolid() && !blacklist.contains(below) && allowedSurfaceMaterials.contains(below)) {
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        FileConfiguration config = plugin.getConfig();
        FileConfiguration langConfig = plugin.getLangConfig();
        boolean allowMovementDuringDelay = config.getBoolean("allow_movement_during_delay", false);

        if (!allowMovementDuringDelay && plugin.getInitialLocationMap().containsKey(playerId)) {
            Location initialLocation = plugin.getInitialLocationMap().get(playerId);
            if (initialLocation != null && player.getWorld().equals(initialLocation.getWorld())) {
                if (player.getLocation().distanceSquared(initialLocation) > 0.1) {
                    player.sendMessage(plugin.colorize(langConfig.getString("moved_during_delay", "You moved during the teleportation delay. Teleportation cancelled.")));
                    plugin.getInitialLocationMap().remove(playerId);
                    plugin.cooldownMap.remove(playerId);

                    // Cancel the teleport task
                    BukkitRunnable teleportTask = plugin.getTeleportTasks().remove(playerId);
                    if (teleportTask != null) {
                        teleportTask.cancel();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Remove the player's initial location and cooldown
        plugin.getInitialLocationMap().remove(playerId);
        plugin.cooldownMap.remove(playerId);

        // Cancel the teleport task
        BukkitRunnable teleportTask = plugin.getTeleportTasks().remove(playerId);
        if (teleportTask != null) {
            teleportTask.cancel();
        }
    }
}