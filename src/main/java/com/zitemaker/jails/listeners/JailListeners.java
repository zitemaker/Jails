package com.zitemaker.jails.listeners;

import com.zitemaker.jails.JailsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class JailListeners implements Listener {

    private final JailsPlugin plugin;

    public JailListeners(JailsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.isPlayerJailed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot break blocks while in jail!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.isPlayerJailed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot place blocks while in jail!");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        if (plugin.isPlayerJailed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot attack while in jail!");
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) return;

        Player player = (Player) event.getEntered();
        if (plugin.isPlayerJailed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot enter vehicles while in jail!");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (plugin.isPlayerJailed(player.getUniqueId())) {
            String jailName = plugin.getJailedPlayersConfig().getString(player.getUniqueId().toString() + ".jailName");
            if (jailName != null) {
                Location jailLoc = plugin.getJail(jailName);
                if (jailLoc != null) {
                    event.setRespawnLocation(jailLoc);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (plugin.isPlayerJailed(playerUUID)) {
            FileConfiguration jailedPlayersConfig = plugin.getJailedPlayersConfig();
            String jailName = jailedPlayersConfig.getString(playerUUID.toString() + ".jailName");

            if (jailName == null) {
                player.sendMessage(ChatColor.RED + "Jail data missing! Contact an admin.");
                return;
            }

            Location jailLocation = plugin.getJail(jailName);
            if (jailLocation == null) {
                player.sendMessage(ChatColor.RED + "Jail location not found! Contact an admin.");
                return;
            }

            long endTime = jailedPlayersConfig.getLong(playerUUID.toString() + ".endTime");
            if (endTime == -1) {
                player.teleport(jailLocation);
                player.sendMessage(ChatColor.RED + "You are permanently jailed by: " +
                        ChatColor.GOLD + jailedPlayersConfig.getString(playerUUID.toString() + ".jailer") +
                        ChatColor.RED + ". Reason: " +
                        ChatColor.YELLOW + jailedPlayersConfig.getString(playerUUID.toString() + ".reason"));
            } else if (System.currentTimeMillis() >= endTime) {
                plugin.unjailPlayer(playerUUID);
                player.sendMessage(ChatColor.GREEN + "Your jail time has ended. You are now free!");
            } else {
                player.teleport(jailLocation);
                long timeLeftMillis = endTime - System.currentTimeMillis();
                String formattedTime = formatTimeLeft(timeLeftMillis);
                player.sendMessage(ChatColor.RED + "You are jailed for " + formattedTime);
            }
        }
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (plugin.isPlayerJailed(player.getUniqueId()) && item != null &&
                item.getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot use ender pearls while in jail!");
        }
    }

    private String formatTimeLeft(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + " second" + (seconds == 1 ? "" : "s");

        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " minute" + (minutes == 1 ? "" : "s");

        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s");

        long days = hours / 24;
        return days + " day" + (days == 1 ? "" : "s");
    }
}