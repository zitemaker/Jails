package com.zitemaker.jails.commands.listeners;

import com.zitemaker.jails.JailsFree;
import com.zitemaker.jails.translation.TranslationManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
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

import java.util.Objects;
import java.util.UUID;

public class JailListeners implements Listener {
    private final JailsFree plugin;
    private final TranslationManager translationManager;

    public JailListeners(JailsFree plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.isPlayerJailed(player.getUniqueId()) &&
                !plugin.getConfig().getBoolean("jails-restrictions.block-break", false)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + translationManager.getMessage("block_break").replace("{prefix}", plugin.getPrefix()));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.isPlayerJailed(player.getUniqueId()) &&
                !plugin.getConfig().getBoolean("jails-restrictions.block-place", false)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + translationManager.getMessage("block_place").replace("{prefix}", plugin.getPrefix()));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        if (plugin.isPlayerJailed(player.getUniqueId()) &&
                !plugin.getConfig().getBoolean("jails-restrictions.attack", false)) {
            event.setCancelled(true);
            player.sendMessage(translationManager.getMessage("attack").replace("{prefix}", plugin.getPrefix()));
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;

        if (plugin.isPlayerJailed(player.getUniqueId()) &&
                !plugin.getConfig().getBoolean("jails-restrictions.vehicle-ride", false)) {
            event.setCancelled(true);
            player.sendMessage(translationManager.getMessage("vehicles").replace("{prefix}", plugin.getPrefix()));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (plugin.isPlayerJailed(player.getUniqueId())) {
            String jailName = plugin.getJailedPlayersConfig().getString(player.getUniqueId() + ".jailName");
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
            String jailName = jailedPlayersConfig.getString(playerUUID + ".jailName");

            if (jailName == null) {
                player.sendMessage(ChatColor.RED + "Jail data missing! Contact an admin.");
                return;
            }

            Location jailLocation = plugin.getJail(jailName);
            if (jailLocation == null) {
                player.sendMessage(ChatColor.RED + "Jail location not found! Contact an admin.");
                return;
            }

            long endTime = jailedPlayersConfig.getLong(playerUUID + ".endTime");
            if (endTime == -1) {
                player.teleport(jailLocation);
                player.sendMessage(ChatColor.RED + translationManager.getMessage("perm_jail")
                        .replace("{prefix}", plugin.getPrefix())
                        .replace("{jailer}", Objects.requireNonNull(jailedPlayersConfig.getString(playerUUID + ".jailer")))
                        .replace("{reason}", Objects.requireNonNull(jailedPlayersConfig.getString(playerUUID + ".reason"))));
            } else if (System.currentTimeMillis() >= endTime) {
                plugin.unjailPlayer(playerUUID);
                player.sendMessage(ChatColor.GREEN + translationManager.getMessage("jail_end").replace("{prefix}", plugin.getPrefix()));
            } else {
                player.teleport(jailLocation);
                long timeLeftMillis = endTime - System.currentTimeMillis();
                String formattedTime = plugin.formatTimeLeft(timeLeftMillis);
                player.sendMessage(ChatColor.RED + translationManager.getMessage("temp_jail")
                        .replace("{prefix}", plugin.getPrefix())
                        .replace("{jailer}", Objects.requireNonNull(jailedPlayersConfig.getString(playerUUID + ".jailer")))
                        .replace("{duration}", formattedTime)
                        .replace("{reason}", Objects.requireNonNull(jailedPlayersConfig.getString(playerUUID + ".reason"))));
            }
        }
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (plugin.isPlayerJailed(player.getUniqueId()) &&
                item != null &&
                item.getType() == Material.ENDER_PEARL &&
                !plugin.getConfig().getBoolean("jails-restrictions.ender-pearl", false)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + translationManager.getMessage("pearl").replace("{prefix}", plugin.getPrefix()));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event){
        Player player = event.getPlayer();
        if(plugin.isPlayerJailed(player.getUniqueId())){
            FileConfiguration jailedPlayersConfig = plugin.getJailedPlayersConfig();
            String jailName = jailedPlayersConfig.getString(player.getUniqueId() + ".jailName");
            Location jailLocation = plugin.getJail(jailName);
            player.teleport(jailLocation);

        }
    }


}