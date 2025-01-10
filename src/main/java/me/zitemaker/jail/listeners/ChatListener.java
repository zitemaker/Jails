package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
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
        String format = event.getFormat();


        if (plugin.isPlayerJailed(player.getUniqueId())) {

            String jailedName = "§c[Jailed]§r " + player.getName();
            event.setFormat(jailedName + ": " + event.getMessage());
        }
    }
}
