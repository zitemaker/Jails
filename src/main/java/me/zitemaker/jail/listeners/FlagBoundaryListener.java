package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlagBoundaryListener implements Listener {

    private final JailPlugin plugin;
    private final Map<UUID, Long> alertCooldown = new HashMap<>();
    private static final long COOLDOWN_TIME = 5000; // 5 seconds cooldown

    public FlagBoundaryListener(JailPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Debug message to check if event is firing
        plugin.getLogger().info("Player " + player.getName() + " moved");

        if (!plugin.isPlayerJailed(player.getUniqueId())) {
            return;
        }

        // Debug message to check if player is jailed
        plugin.getLogger().info("Player " + player.getName() + " is jailed");

        FileConfiguration jailConfig = plugin.getJailedPlayersConfig();
        String jailName = jailConfig.getString(player.getUniqueId().toString() + ".jailName");

        if (jailName == null) {
            plugin.getLogger().warning("No jail name found for player " + player.getName());
            return;
        }

        // Get jail location and bounds
        Location jailLoc = plugin.getJail(jailName);
        if (jailLoc == null) {
            plugin.getLogger().warning("No jail location found for jail " + jailName);
            return;
        }

        // Define jail bounds (for example, 10 blocks in each direction from jail point)
        int radius = 10;
        double minX = jailLoc.getX() - radius;
        double maxX = jailLoc.getX() + radius;
        double minY = jailLoc.getY() - radius;
        double maxY = jailLoc.getY() + radius;
        double minZ = jailLoc.getZ() - radius;
        double maxZ = jailLoc.getZ() + radius;

        Location playerLoc = player.getLocation();

        // Check if player is outside bounds
        if (playerLoc.getX() < minX || playerLoc.getX() > maxX ||
                playerLoc.getY() < minY || playerLoc.getY() > maxY ||
                playerLoc.getZ() < minZ || playerLoc.getZ() > maxZ ||
                !playerLoc.getWorld().equals(jailLoc.getWorld())) {

            // Debug message
            plugin.getLogger().info("Player " + player.getName() + " is outside jail bounds");

            // Check cooldown
            long currentTime = System.currentTimeMillis();
            if (!alertCooldown.containsKey(player.getUniqueId()) ||
                    currentTime - alertCooldown.get(player.getUniqueId()) > COOLDOWN_TIME) {

                // Broadcast escape message
                Bukkit.broadcastMessage(ChatColor.RED + "[ALERT] " + player.getName() +
                        " has attempted to escape from jail!");

                // Teleport back to jail
                player.teleport(jailLoc);
                player.sendMessage(ChatColor.RED + "You cannot escape from jail!");

                // Set cooldown
                alertCooldown.put(player.getUniqueId(), currentTime);

                // Schedule removal message
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.broadcastMessage(ChatColor.RED + "[ALERT] " + player.getName() +
                                " has been caught and returned to jail!");
                    }
                }.runTaskLater(plugin, 100L); // 5 seconds
            }
        }
    }
}