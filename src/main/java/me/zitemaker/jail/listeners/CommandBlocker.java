package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;

public class CommandBlocker implements Listener {

    private final JailPlugin plugin;

    public CommandBlocker(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();


        String[] commandParts = command.split(" ");
        String baseCommand = commandParts[0].substring(1);


        if (plugin.isPlayerJailed(player.getUniqueId()) && plugin.blockedCommands.contains(baseCommand)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot use this command while jailed.");
        }
    }
}
