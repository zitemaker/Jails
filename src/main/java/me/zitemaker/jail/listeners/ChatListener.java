package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;

public class ChatListener implements Listener {
    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public ChatListener(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.isPlayerJailed(player.getUniqueId())) {
            if (!plugin.getConfig().getBoolean("jail-restrictions.chat", false)) {
                String prefix = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("prefix", "&7[&eJailsPlus&7]"));
                player.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("chat"));
                event.setCancelled(true);
                return;
            }

            if (plugin.getConfig().getBoolean("jail-settings.enable-jailed-role")) {
                String jailedRole = plugin.getConfig().getString("jail-settings.jailed-role", "§c[Jailed]§r");
                jailedRole = ChatColor.translateAlternateColorCodes('&', jailedRole);

                event.setFormat(jailedRole + " " + "%1$s: %2$s");
            }
        }
    }
}