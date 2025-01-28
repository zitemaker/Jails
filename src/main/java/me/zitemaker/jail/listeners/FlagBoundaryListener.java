package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;

public class FlagBoundaryListener implements Listener {

    private final JailPlugin plugin;
    private final Map<UUID, Long> alertCooldown;
    private final Set<UUID> alreadyAlerted;
    private final Set<String> notifiedInsecureJails = new HashSet<>();

    public FlagBoundaryListener(JailPlugin plugin) {
        this.plugin = plugin;
        this.alertCooldown = plugin.getAlertCooldown();
        this.alreadyAlerted = plugin.getAlreadyAlerted();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (plugin.isPlayerJailed(playerUUID)) {

            if (!plugin.getConfig().getBoolean("jail-restrictions.allow-movement", true)) {
                Location from = event.getFrom();
                Location to = event.getTo();

                if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You cannot move while in jail!");
                    return;
                }
            }

            String jailName = plugin.getJailedPlayersConfig().getString(playerUUID.toString() + ".jailName");
            if (jailName != null) {
                Location jailLocation = plugin.getJail(jailName);
                if (jailLocation != null) {
                    if (!notifiedInsecureJails.contains(jailName)) {
                        plugin.notifyInsecureJail(jailName, jailLocation, player);
                    }

                    if (plugin.getConfig().getBoolean("punish-on-escape", true)) {
                        Location from = event.getFrom();
                        Location to = event.getTo();

                        if (from.getBlockX() != to.getBlockX() ||
                                from.getBlockY() != to.getBlockY() ||
                                from.getBlockZ() != to.getBlockZ()) {

                            boolean wasInside = isInsideJailArea(from, jailLocation);
                            boolean isInside = isInsideJailArea(to, jailLocation);

                            if (wasInside && !isInside) {
                                //handleEscapePunishment(player, playerUUID);
                                plugin.handleEscape(player, playerUUID);
                            }
                        }
                    }
                }
            }

            plugin.handleBoundaryCheck(player, playerUUID);
        } else {
            alertCooldown.remove(playerUUID);
            alreadyAlerted.remove(playerUUID);
        }
    }

    private boolean isInsideJailArea(Location location, Location jailCenter) {
        int radius = plugin.getConfig().getInt("jail-radius", 5);
        double dx = location.getX() - jailCenter.getX();
        double dz = location.getZ() - jailCenter.getZ();
        double distanceSquared = dx * dx + dz * dz;
        double radiusSquared = radius * radius;

        return distanceSquared <= radiusSquared &&
                Math.abs(location.getY() - jailCenter.getY()) <= plugin.getConfig().getInt("jail-height", 5);
    }

    private void handleEscapePunishment(Player player, UUID playerUUID) {
        String punishment = plugin.getConfig().getString("escape_punishment", "KILL");
        String message = plugin.getConfig().getString("escape-punishment-message", "{prefix} &cYou were punished for trying to escape jail!");
        message = ChatColor.translateAlternateColorCodes('&', message.replace("{prefix}", plugin.getPrefix()));

        switch (punishment.toUpperCase()) {
            case "KILL":
                player.setHealth(0);
                break;
            case "TELEPORT_BACK":
                String jailName = plugin.getJailedPlayersConfig().getString(playerUUID.toString() + ".jailName");
                Location jailLocation = plugin.getJail(jailName);
                if (jailLocation != null) {
                    player.teleport(jailLocation);
                }
                break;
            case "BAN":
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                        player.getName(),
                        "Attempted to escape from jail",
                        null,
                        null
                );
                player.kickPlayer("You have been banned for attempting to escape from jail");
                break;
            case "UNJAIL":
                plugin.playerEscape(playerUUID);
                message = ChatColor.translateAlternateColorCodes('&',
                        plugin.getPrefix() + " &aYou have successfully escaped from jail!");
                break;
            default:
                return;
        }

        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (alreadyAlerted.contains(playerUUID)) {
            alreadyAlerted.remove(playerUUID);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (alreadyAlerted.contains(playerUUID)) {
            alreadyAlerted.remove(playerUUID);
        }
    }
}