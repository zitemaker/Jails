package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class UnjailCommand implements CommandExecutor {

    private final JailPlugin plugin;

    public UnjailCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jailplugin.unjail")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unjail <player>");
            return true;
        }


        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + "Player not found or has never joined the server.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();

        if (!plugin.isPlayerJailed(targetUUID)) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " is not jailed.");
            return true;
        }


        plugin.unjailPlayer(targetUUID);

        sender.sendMessage(ChatColor.GREEN + "Player " + target.getName() + " has been unjailed.");
        Bukkit.broadcastMessage(ChatColor.GREEN + target.getName() + " has been unjailed.");
        return true;
    }
}
