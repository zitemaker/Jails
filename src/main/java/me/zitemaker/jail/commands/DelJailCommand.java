package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DelJailCommand implements CommandExecutor {

    private final JailPlugin plugin;

    public DelJailCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jails.deljail")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /deljail <jail name>");
            return true;
        }

        String jailName = args[0].toLowerCase();
        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(ChatColor.RED + "Jail '" + jailName + "' does not exist.");
            return true;
        }

        plugin.removeJail(jailName);
        sender.sendMessage(ChatColor.GREEN + "Jail " + ChatColor.YELLOW + jailName + ChatColor.GREEN + " has been successfully deleted.");
        return true;
    }
}