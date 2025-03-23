package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;

public class ChatListener implements Listener {
    private final JailPlugin plugin;

    public ChatListener(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.isPlayerJailed(player.getUniqueId())) {
            // check if chat is disabled for jailed players
            if (!plugin.getConfig().getBoolean("jail-restrictions.chat", false)) {
                String prefix = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("prefix", "&7[&eJailsPlus&7]"));
                player.sendMessage(prefix + " " + ChatColor.RED + "You cannot chat while jailed!");
                event.setCancelled(true);
                return;
            }

            // if chat is allowed give the jailed players jailed role
            if (plugin.getConfig().getBoolean("jail-settings.enable-jailed-role")) {
                String jailedRole = plugin.getConfig().getString("jail-settings.jailed-role", "§c[Jailed]§r");
                jailedRole = ChatColor.translateAlternateColorCodes('&', jailedRole);

                event.setFormat(jailedRole + " " + "%1$s: %2$s");
            }
        }
    }
}