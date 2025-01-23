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
    private static final long COOLDOWN_TIME = 5000;

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

        if (!plugin.isPlayerJailed(playerUUID)) {
            alertCooldown.remove(playerUUID);
            alreadyAlerted.remove(playerUUID);
            return;
        }

        String jailName = plugin.getJailedPlayersConfig().getString(playerUUID.toString() + ".jailName");
        if (jailName != null) {
            Location jailLocation = plugin.getJail(jailName);
            if (jailLocation != null) {
                plugin.notifyInsecureJail(jailName, jailLocation, player);
            }
        }

        plugin.handleBoundaryCheck(player, playerUUID);
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