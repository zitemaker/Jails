package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ConfigReload implements CommandExecutor {
    private final JailPlugin plugin;

    public ConfigReload(JailPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("jails.reload")) {
            try {
                plugin.reloadPluginConfig();
                sender.sendMessage(plugin.getTranslationManager().getMessage("config_reload_success"));
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to reload config.yml: " + e.getMessage());
                sender.sendMessage(ChatColor.RED + "An error occurred while reloading the configuration.");
            }

            try {
                plugin.getTranslationManager().restoreLanguageFiles();
                plugin.getTranslationManager().reloadMessages();
                sender.sendMessage(plugin.getTranslationManager().getMessage("messages_reload_success"));
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to reload language files: " + e.getMessage());
                sender.sendMessage(ChatColor.RED + "An error occurred while reloading language messages.");
            }

            return true;
        } else {
            sender.sendMessage(plugin.getTranslationManager().getMessage("no_permission"));
            return true;
        }

    }
}