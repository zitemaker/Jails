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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("jails.reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Jails configuration reloaded!");
            return true;
        }
        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
        return true;
    }
}
