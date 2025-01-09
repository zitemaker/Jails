package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class JailListeners implements Listener {

    private final JailPlugin plugin;

    public JailListeners(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        Player player = event.getPlayer();

        event.setCancelled(true);

        String message = ChatColor.GOLD + "You cannot break blocks while in jail!";

        player.sendMessage(message);
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event){
        Player player = (Player) event.getDamager();

        event.setCancelled(true);

        String message = ChatColor.GOLD + "You cannot attack others while in jail!";

        player.sendMessage(message);
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

}
