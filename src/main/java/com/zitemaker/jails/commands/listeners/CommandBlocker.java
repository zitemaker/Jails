package com.zitemaker.jails.commands.listeners;

import com.zitemaker.jails.JailsFree;
import com.zitemaker.jails.translation.TranslationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;

public class CommandBlocker implements Listener {
    private final JailsFree plugin;
    private final TranslationManager translationManager;

    public CommandBlocker(JailsFree plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        String[] commandParts = command.split(" ");
        String baseCommand = commandParts[0].substring(1);

        if (plugin.isPlayerJailed(player.getUniqueId()) && plugin.blockedCommands.contains(baseCommand)) {
            event.setCancelled(true);
            player.sendMessage(translationManager.getMessage("command_usage").replace("{prefix}", plugin.getPrefix()));
        }
    }
}