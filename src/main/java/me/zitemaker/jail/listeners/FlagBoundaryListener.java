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
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.*;

public class FlagBoundaryListener implements Listener {

    private final JailPlugin plugin;
    private final Map<UUID, Long> alertCooldown = new HashMap<>();
    private final Set<UUID> alreadyAlerted = new HashSet<>();
    private static final long COOLDOWN_TIME = 5000;

    public FlagBoundaryListener(JailPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJail(PlayerJailEvent event) {
                alreadyAlerted.remove(event.getPlayer().getUniqueId());
                alertCooldown.remove(event.getPlayer().getUniqueId());
            }
        }, plugin);
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

        FileConfiguration flagsConfig = plugin.getFlagsConfig();
        boolean isOutsideBoundary = false;

        for (String flagName : flagsConfig.getKeys(false)) {
            String worldName = flagsConfig.getString(flagName + ".world");
            String[] pos1 = flagsConfig.getString(flagName + ".pos1").split(",");
            String[] pos2 = flagsConfig.getString(flagName + ".pos2").split(",");

            if (!player.getWorld().getName().equals(worldName)) {
                continue;
            }

            int minX = Integer.parseInt(pos1[0]);
            int minY = Integer.parseInt(pos1[1]);
            int minZ = Integer.parseInt(pos1[2]);
            int maxX = Integer.parseInt(pos2[0]);
            int maxY = Integer.parseInt(pos2[1]);
            int maxZ = Integer.parseInt(pos2[2]);

            Location playerLoc = player.getLocation();

            if (playerLoc.getX() < minX || playerLoc.getX() > maxX ||
                    playerLoc.getY() < minY || playerLoc.getY() > maxY ||
                    playerLoc.getZ() < minZ || playerLoc.getZ() > maxZ) {

                isOutsideBoundary = true;
                break;
            }
        }

        if (!isOutsideBoundary) {
            alreadyAlerted.remove(playerUUID);
            alertCooldown.remove(playerUUID);
            return;
        }

        boolean shouldTeleport = plugin.getConfig().getBoolean("jail.jailbreak-tp", true);

        if (shouldTeleport) {
            handleTeleportBack(player, playerUUID);
        } else {
            handleEscape(player, playerUUID);
        }
    }

    private void handleTeleportBack(Player player, UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        if (!alertCooldown.containsKey(playerUUID) ||
                currentTime - alertCooldown.get(playerUUID) > COOLDOWN_TIME) {

            String jailName = plugin.getJailedPlayersConfig().getString(playerUUID.toString() + ".jailName");
            if (jailName != null) {
                Location jailLocation = plugin.getJail(jailName);
                if (jailLocation != null) {
                    player.teleport(jailLocation);
                    player.sendMessage(ChatColor.RED + "You have been returned to your jail cell!");
                    Bukkit.broadcastMessage(ChatColor.RED + "[ALERT] " + player.getName() +
                            " has attempted to escape from jail!");
                }
            }
            alertCooldown.put(playerUUID, currentTime);
        }
    }

    private void handleEscape(Player player, UUID playerUUID) {
        if (!alreadyAlerted.contains(playerUUID)) {
            Location escapeLocation = player.getLocation();

            String originalSpawnOption = plugin.getJailedPlayersConfig().getString(
                    playerUUID.toString() + ".spawnOption");

            //plugin.getJailedPlayersConfig().set(playerUUID.toString() + ".spawnOption", "none");
            //plugin.saveJailedPlayersConfig();

            //plugin.unjailPlayer(playerUUID);

            //player.teleport(escapeLocation);

            Bukkit.broadcastMessage(ChatColor.RED + "[ALERT] " + player.getName() +
                    " has escaped from jail!");

            alreadyAlerted.add(playerUUID);
        }
    }
}