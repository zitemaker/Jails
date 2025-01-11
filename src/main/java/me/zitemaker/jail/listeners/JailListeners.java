package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
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
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class JailListeners implements Listener {

    private final JailPlugin plugin;

    public JailListeners(JailPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isPlayerJailed(Player player) {
        UUID playerUUID = player.getUniqueId();
        return plugin.getJailedPlayersConfig().contains(playerUUID.toString());
    }

    private boolean isActionAllowed(String actionKey) {
        FileConfiguration config = plugin.getConfig();
        return config.getBoolean("jail-restrictions." + actionKey, true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (isPlayerJailed(player) && isActionAllowed("block-break")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.GOLD + "You cannot break blocks while in jail!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (isPlayerJailed(player) && isActionAllowed("block-place")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.GOLD + "You cannot place blocks while in jail!");
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if (damager instanceof Player player && isPlayerJailed(player) && isActionAllowed("attack")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.GOLD + "You cannot attack others while in jail!");
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

            if (jailedPlayersConfig.getBoolean(playerUUID.toString() + ".unjailed", false)) {
                String spawnOption = jailedPlayersConfig.getString(playerUUID.toString() + ".spawnOption", "original_location");

                if (spawnOption.equals("world_spawn")) {
                    Location worldSpawn = player.getWorld().getSpawnLocation();
                    player.teleport(worldSpawn);
                } else if (spawnOption.equals("original_location")) {
                    plugin.teleportToOriginalLocation(player, playerUUID.toString() + ".original");
                }

                jailedPlayersConfig.set(playerUUID.toString(), null);
                plugin.saveJailedPlayersConfig();
                player.sendMessage(ChatColor.GREEN + "You were unjailed.");
                return;
            }

            long endTime = jailedPlayersConfig.getLong(playerUUID.toString() + ".endTime");
            if (endTime == -1) {
                player.teleport(jailLocation);
                player.sendMessage(ChatColor.RED + "You have been permanently jailed by: " + ChatColor.GOLD + jailedPlayersConfig.getString("jailer") + ". Reason: " + ChatColor.YELLOW + jailedPlayersConfig.getString("reason") + ". Appeal in our discord server.");
            } else if (System.currentTimeMillis() >= endTime) {
                plugin.unjailPlayer(playerUUID);
                player.sendMessage(ChatColor.GREEN + "Your jail time has ended. Welcome back!");
            } else {
                player.teleport(jailLocation);
                long timeLeft = (endTime - System.currentTimeMillis()) / 1000;
                player.sendMessage(ChatColor.RED + "You have been temporarily jailed by " + ChatColor.GOLD + jailedPlayersConfig.getString("jailer") + ". Reason: " + ChatColor.YELLOW + jailedPlayersConfig.getString("reason") + ". Duration: " + timeLeft + " seconds. Appeal in our discord.");
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Vehicle && isActionAllowed("vehicle-ride")) {
            if (entity instanceof Player player && isPlayerJailed(player)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.GOLD + "You cannot ride vehicles while in jail!");
            }
        }
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isPlayerJailed(player)) {
            if (item != null && item.getType() == Material.ENDER_PEARL && isActionAllowed("ender-pearl")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.GOLD + "You cannot use ender pearls while in jail!");
            }
        }
    }
}