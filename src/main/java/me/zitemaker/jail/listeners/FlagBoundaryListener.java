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
    private final Set<String> notifiedInsecureJails = new HashSet<>();
    private static final long COOLDOWN_TIME = 5000;

    public FlagBoundaryListener(JailPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!plugin.isPlayerJailed(playerUUID)) {
            alertCooldown.remove(playerUUID);
            alreadyAlerted.remove(playerUUID);
            return;
        }

        String jailName = plugin.getJailedPlayersConfig().getString(playerUUID.toString() + ".jailName");
        if (jailName != null) {
            Location jailLocation = plugin.getJail(jailName);
            if (jailLocation != null) {
                notifyInsecureJail(jailName, jailLocation, player);
            }
        }

        handleBoundaryCheck(player, playerUUID);
    }

    private void notifyInsecureJail(String jailName, Location jailLocation, Player setter) {
        if (!plugin.isLocationInAnyFlag(jailLocation) && !notifiedInsecureJails.contains(jailName)) {
            notifiedInsecureJails.add(jailName);

            setter.sendMessage(ChatColor.RED + "[SECURITY ALERT] The jail '" + jailName +
                    "' is not within a secure flag zone!");

            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[SECURITY BREACH] Jail '" + jailName +
                    "' is not secured within a flagged zone! Immediate attention required.");
        }
    }

    private void handleBoundaryCheck(Player player, UUID playerUUID) {
        FileConfiguration flagsConfig = plugin.getFlagsConfig();
        String assignedFlag = plugin.getJailedPlayersConfig().getString(playerUUID.toString() + ".assignedFlag");
        boolean isInsideAssignedFlag = false;

        if (assignedFlag != null && isPlayerInsideFlag(player.getLocation(), flagsConfig, assignedFlag)) {
            isInsideAssignedFlag = true;
        } else {
            for (String flagName : flagsConfig.getKeys(false)) {
                if (isPlayerInsideFlag(player.getLocation(), flagsConfig, flagName)) {
                    isInsideAssignedFlag = true;
                    plugin.getJailedPlayersConfig().set(playerUUID.toString() + ".assignedFlag", flagName);
                    plugin.saveJailedPlayersConfig();
                    break;
                }
            }
        }

        if (!isInsideAssignedFlag) {
            boolean shouldTeleport = plugin.getConfig().getBoolean("jail.jailbreak-tp", true);
            if (shouldTeleport) {
                handleTeleportBack(player, playerUUID);
            } else {
                handleEscape(player, playerUUID);
            }
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
        if (!alertCooldown.containsKey(playerUUID) || currentTime - alertCooldown.get(playerUUID) > COOLDOWN_TIME) {
            String jailName = plugin.getJailedPlayersConfig().getString(playerUUID.toString() + ".jailName");
            Location jailLocation = jailName != null ? plugin.getJail(jailName) : null;

            if (jailLocation != null && plugin.isLocationInAnyFlag(jailLocation)) {
                player.teleport(jailLocation);
                player.sendMessage(ChatColor.RED + "You have been returned to your jail cell!");
                Bukkit.broadcastMessage(ChatColor.DARK_RED + "[Security Alert] " +
                        ChatColor.GOLD + player.getName() + ChatColor.RED + " attempted to escape and was returned.");
            } else {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[CRITICAL ERROR] Cannot return prisoner " +
                        player.getName() + " to jail '" + jailName + "' as it's not within a secure zone!");
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