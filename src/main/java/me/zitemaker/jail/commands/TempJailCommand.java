package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class TempJailCommand implements CommandExecutor {
    private final JailPlugin plugin;

    public TempJailCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /tempjail <player> <jail name> <duration (e.g., 2d, 3h)> [reason]");
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return false;
        }

        String jailName = args[1];
        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(ChatColor.RED + "Jail not found.");
            return false;
        }

        long duration = JailPlugin.parseDuration(args[2]);
        if (duration <= 0) {
            sender.sendMessage(ChatColor.RED + "Invalid duration.");
            return false;
        }

        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "No reason provided";

        Location jailLocation = plugin.getJails().get(jailName);
        target.teleport(jailLocation);

        FileConfiguration config = plugin.getJailedPlayersConfig();
        config.set(target.getUniqueId() + ".jailName", jailName);
        config.set(target.getUniqueId() + ".endTime", System.currentTimeMillis() + duration);
        config.set(target.getUniqueId() + ".reason", reason);
        config.set(target.getUniqueId() + ".jailer", sender.getName());
        plugin.saveJailedPlayersConfig();

        String broadcastMessage = ChatColor.RED + target.getName() + " has been jailed by " + sender.getName() +
                " for " + args[2] + ". Reason: " + reason;
        Bukkit.broadcastMessage(broadcastMessage);

        target.sendMessage(ChatColor.RED + "You have been jailed by " + sender.getName() +
                " for " + args[2] + ". Reason: " + reason);

        plugin.scheduleUnjail(target, duration);
        return true;
    }
}