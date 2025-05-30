package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ConfigReload implements CommandExecutor {
    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public ConfigReload(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (sender.hasPermission("jails.reload")) {
            plugin.reloadPluginConfig();
            sender.sendMessage(prefix + " " + ChatColor.GREEN + translationManager.getMessage("config_reload_success"));
        } else {
            sender.sendMessage(prefix + " " + ChatColor.GREEN + translationManager.getMessage("cr_no_permission"));
        }
        return true;
    }
}
