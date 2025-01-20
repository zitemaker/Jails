package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class FlagBoundaryListener implements Listener {

    private final JailPlugin plugin;
    private final Map<UUID, Long> alertCooldown = new HashMap<>();
    private final Set<UUID> alreadyAlerted = new HashSet<>();
    private final Map<String, Long> jailWarningCooldown = new HashMap<>();
    private static final long COOLDOWN_TIME = 5000;
    private static final long JAIL_WARNING_COOLDOWN = 300000;

    public FlagBoundaryListener(JailPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!plugin.isPlayerJailed(playerUUID)) {
            alreadyAlerted.remove(playerUUID);
            alertCooldown.remove(playerUUID);
            return;
        }

        String jailName = plugin.getJailedPlayersConfig().getString(playerUUID.toString() + ".jailName");
        if (jailName != null) {
            Location jailLocation = plugin.getJail(jailName);
            if (jailLocation != null && !plugin.isLocationInAnyFlag(jailLocation)) {
                long currentTime = System.currentTimeMillis();
                if (!jailWarningCooldown.containsKey(jailName) ||
                        currentTime - jailWarningCooldown.get(jailName) > JAIL_WARNING_COOLDOWN) {

                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[SECURITY BREACH] " +
                            ChatColor.GOLD + "Prison Security Alert: " + ChatColor.RED +
                            "Prisoner " + ChatColor.YELLOW + player.getName() + ChatColor.RED +
                            " is being held in jail '" + ChatColor.YELLOW + jailName + ChatColor.RED +
                            "' which is not within any security zone (flag)! Immediate action required!");

                    for (Player staff : Bukkit.getOnlinePlayers()) {
                        if (staff.hasPermission("jailplugin.admin")) {
                            staff.sendMessage(ChatColor.RED + "[SECURITY WARNING] " +
                                    ChatColor.GOLD + "Alert: " + ChatColor.RED +
                                    "Jail '" + jailName + "' containing prisoner " + player.getName() +
                                    " is not within a secured zone!");
                        }
                    }
                    jailWarningCooldown.put(jailName, currentTime);
                }
            }
        }

        FileConfiguration flagsConfig = plugin.getFlagsConfig();
        boolean isOutsideBoundary = true;
        String assignedFlag = null;

        for (String flagName : flagsConfig.getKeys(false)) {
            if (isPlayerInsideFlag(player.getLocation(), flagsConfig, flagName)) {
                isOutsideBoundary = false;
                assignedFlag = flagName;
                break;
            }
        }

        if (!isOutsideBoundary) {
            alreadyAlerted.remove(playerUUID);
            alertCooldown.remove(playerUUID);
            plugin.getJailedPlayersConfig().set(playerUUID.toString() + ".assignedFlag", assignedFlag);
            plugin.saveJailedPlayersConfig();
            return;
        }

        boolean shouldTeleport = plugin.getConfig().getBoolean("jail.jailbreak-tp", true);

        if (shouldTeleport) {
            handleTeleportBack(player, playerUUID);
        } else {
            handleEscape(player, playerUUID);
        }
    }

    private boolean isPlayerInsideFlag(Location location, FileConfiguration flagsConfig, String flagName) {
        String worldName = flagsConfig.getString(flagName + ".world");
        String pos1String = flagsConfig.getString(flagName + ".pos1");
        String pos2String = flagsConfig.getString(flagName + ".pos2");

        if (worldName == null || pos1String == null || pos2String == null) return false;
        if (!location.getWorld().getName().equals(worldName)) return false;

        try {
            String[] pos1 = pos1String.split(",");
            String[] pos2 = pos2String.split(",");

            int minX = Math.min(Integer.parseInt(pos1[0]), Integer.parseInt(pos2[0]));
            int minY = Math.min(Integer.parseInt(pos1[1]), Integer.parseInt(pos2[1]));
            int minZ = Math.min(Integer.parseInt(pos1[2]), Integer.parseInt(pos2[2]));
            int maxX = Math.max(Integer.parseInt(pos1[0]), Integer.parseInt(pos2[0]));
            int maxY = Math.max(Integer.parseInt(pos1[1]), Integer.parseInt(pos2[1]));
            int maxZ = Math.max(Integer.parseInt(pos1[2]), Integer.parseInt(pos2[2]));

            return location.getX() >= minX && location.getX() <= maxX &&
                    location.getY() >= minY && location.getY() <= maxY &&
                    location.getZ() >= minZ && location.getZ() <= maxZ;
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid flag coordinates for flag '" + flagName + "': " + e.getMessage());
            return false;
        }
    }

    private void handleTeleportBack(Player player, UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        if (!alertCooldown.containsKey(playerUUID) ||
                currentTime - alertCooldown.get(playerUUID) > COOLDOWN_TIME) {

            String jailName = plugin.getJailedPlayersConfig().getString(playerUUID.toString() + ".jailName");
            if (jailName != null) {
                Location jailLocation = plugin.getJail(jailName);
                if (jailLocation != null && plugin.isLocationInAnyFlag(jailLocation)) {
                    player.teleport(jailLocation);
                    player.sendMessage(ChatColor.RED + "You have been returned to your jail cell!");
                    Bukkit.broadcastMessage(ChatColor.DARK_RED + "[Security Alert] " + ChatColor.GOLD + player.getName() +
                            ChatColor.RED + " attempted to escape from jail and has been returned.");
                } else {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[CRITICAL ERROR] " +
                            ChatColor.GOLD + "Security Breach: " + ChatColor.RED +
                            "Cannot return prisoner " + player.getName() + " to jail '" + jailName +
                            "' as it's not within a secure zone!");
                }
            }
            alertCooldown.put(playerUUID, currentTime);
        }
    }

    private void handleEscape(Player player, UUID playerUUID) {
        if (!alreadyAlerted.contains(playerUUID)) {
            String jailName = plugin.getJailedPlayersConfig().getString(playerUUID.toString() + ".jailName");
            if (jailName != null) {
                Location jailLocation = plugin.getJail(jailName);
                if (jailLocation != null && plugin.isLocationInAnyFlag(jailLocation)) {
                    Location escapeLocation = player.getLocation();

                    String originalSpawnOption = plugin.getJailedPlayersConfig().getString(
                            playerUUID.toString() + ".spawnOption");

                    plugin.getJailedPlayersConfig().set(playerUUID.toString() + ".spawnOption", "none");
                    plugin.saveJailedPlayersConfig();

                    plugin.playerEscape(playerUUID);

                    player.teleport(escapeLocation);

                    Bukkit.broadcastMessage(ChatColor.RED + "[Alert] " + ChatColor.GOLD + player.getName() +
                            ChatColor.RED + " has escaped from jail! Security breach detected!");

                    alreadyAlerted.add(playerUUID);
                } else {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[SECURITY BREACH] " +
                            ChatColor.GOLD + "Critical Alert: " + ChatColor.RED +
                            "Prisoner " + player.getName() + " attempted escape from unsecured jail '" +
                            jailName + "'! Immediate action required!");
                }
            }
        }
    }
}