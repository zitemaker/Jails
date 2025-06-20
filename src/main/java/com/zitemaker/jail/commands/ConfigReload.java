package com.zitemaker.jail.commands;

import com.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ConfigReload implements CommandExecutor {
    private final JailPlugin plugin;

    public ConfigReload(JailPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String prefix = plugin.getPrefix();
        if (sender.hasPermission("jails.reload")) {
            try {
                plugin.reloadPluginConfig();
                sender.sendMessage(plugin.getTranslationManager().getMessage("config_reload_success").replace("{prefix}", prefix));
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to reload config.yml: " + e.getMessage());
                sender.sendMessage(ChatColor.RED + "An error occurred while reloading the configuration.");
            }
        } else {
            sender.sendMessage(plugin.getTranslationManager().getMessage("cr_no_permission").replace("{prefix}", prefix));
            return true;
        }
        return true;
    }
}