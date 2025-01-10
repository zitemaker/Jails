package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
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

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (isPlayerJailed(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.GOLD + "You cannot break blocks while in jail!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (isPlayerJailed(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.GOLD + "You cannot place blocks while in jail!");
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if (damager instanceof Player player && isPlayerJailed(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.GOLD + "You cannot attack others while in jail!");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String basePath = playerUUID.toString() + ".original";

        if (plugin.getJailedPlayersConfig().contains(basePath)) {
            plugin.teleportToOriginalLocation(player, basePath);
            plugin.getJailedPlayersConfig().set(playerUUID.toString(), null);
            plugin.saveJailedPlayersConfig();
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Vehicle && (entity instanceof org.bukkit.entity.Boat || entity instanceof org.bukkit.entity.Minecart)) {
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
            if (item != null && item.getType() == Material.ENDER_PEARL) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.GOLD + "You cannot use ender pearls while in jail!");
            }
        }
    }
}