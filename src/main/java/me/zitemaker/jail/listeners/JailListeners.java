package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
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

        if(plugin.isPlayerJailed(player.getUniqueId())){
            event.setCancelled(true);

            String message = ChatColor.GOLD + "You cannot break blocks while in jail!";

            player.sendMessage(message);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();


            if (plugin.isPlayerJailed(player.getUniqueId())) {
                event.setCancelled(true);
                String message = ChatColor.GOLD + "You cannot attack others while in jail!";
                player.sendMessage(message);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (plugin.isPlayerJailed(playerUUID)) {
            String jailName = plugin.getJailedPlayersConfig().getString(playerUUID.toString() + ".jailName");
            long endTime = plugin.getJailedPlayersConfig().getLong(playerUUID.toString() + ".endTime");

            if (jailName != null) {
                if(endTime == -1){
                    Location jailLocation = plugin.getJail(jailName);
                    player.teleport(jailLocation);
                    player.sendMessage(ChatColor.RED + "You have been permanently jailed. Appeal in our discord server to get unjailed.");
                }
                else if (System.currentTimeMillis() >= endTime) {

                    plugin.unjailPlayer(playerUUID);
                    player.sendMessage(ChatColor.GREEN + "Your jail time has ended. Welcome back!");
                }
                else {

                    Location jailLocation = plugin.getJail(jailName);
                    if (jailLocation != null) {
                        player.teleport(jailLocation);
                        player.sendMessage(ChatColor.RED + "You are still jailed! Time left: "
                                + ((endTime - System.currentTimeMillis()) / 1000) + " seconds.");
                    } else {
                        player.sendMessage(ChatColor.RED + "Jail location not found! Contact an admin.");
                    }
                }
            }
        }
    }


}



