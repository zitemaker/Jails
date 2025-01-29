package com.zitemaker.jails.listeners;

import com.zitemaker.jails.JailsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;

public class ChatListener implements Listener {
    private final JailsPlugin plugin;

    public ChatListener(JailsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String format = event.getFormat();


        if (plugin.isPlayerJailed(player.getUniqueId())) {

            String jailedName = "§c[Jailed]§r " + player.getName();
            event.setFormat(jailedName + ": " + event.getMessage());
        }
    }
}
