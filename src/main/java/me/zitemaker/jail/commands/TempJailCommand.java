package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

        if(plugin.isPlayerJailed(target.getUniqueId())){
            sender.sendMessage(ChatColor.RED + target.getName() + " is already jailed!");
            return false;
        }

        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "No reason provided";



        long durationMillis = duration;
        long endTime = System.currentTimeMillis() + durationMillis;


        String formattedTime = plugin.formatTimeLeft(durationMillis);


        plugin.jailPlayer(target, jailName, endTime, reason, sender.getName());
        plugin.scheduleUnjail(target, durationMillis);


        target.sendMessage(ChatColor.RED + "You have been jailed in " + jailName + " for " + formattedTime + ". Reason: " + reason);


        String prefix = plugin.getConfig().getString("prefix", "&7[&eJails&7]");
        String messageTemplate = plugin.getConfig().getString("general.jail-broadcast-message",
                "{prefix} &c{player} has been jailed for {duration} by {jailer}. Reason: {reason}!");


        String broadcastMessage = messageTemplate
                .replace("{prefix}", ChatColor.translateAlternateColorCodes('&', prefix))
                .replace("{player}", target.getName())
                .replace("{duration}", formattedTime)
                .replace("{jailer}", sender.getName())
                .replace("{reason}", reason);


        if(plugin.getConfig().getBoolean("general.broadcast-on-jail")){
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
        } else {
            sender.sendMessage(ChatColor.GREEN + target.getName() + " has been jailed temporarily for " + formattedTime + ".");
        }


        return true;
    }
}