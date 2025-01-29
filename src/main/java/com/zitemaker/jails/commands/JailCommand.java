package com.zitemaker.jails.commands;

import com.zitemaker.jails.JailsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class JailCommand implements CommandExecutor {

    private final JailsPlugin plugin;

    public JailCommand(JailsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jailplugin.jail")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /jail <player> <jail name> [reason]");
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return false;
        }

        String jailName = args[1];
        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(ChatColor.RED + "Jail not found. Available jails:");
            plugin.getJails().forEach((name, location) ->
                    sender.sendMessage(ChatColor.GOLD + "- " + name)
            );
            return false;
        }

        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "No reason provided";

        Location jailLocation = plugin.getJails().get(jailName);
        plugin.jailPlayer(target, jailName, -1, reason, sender.getName());

        String broadcastMessage = ChatColor.RED + target.getName() + " has been jailed by " + sender.getName() +
                " permanently. Reason: " + reason;
        Bukkit.broadcastMessage(broadcastMessage);

        target.sendMessage(ChatColor.RED + "You have been jailed permanently by " + sender.getName() +
                ". Reason: " + reason);
        return true;
    }
}