package me.zitemaker.jail.ipjail;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

public class IPJailCommand implements CommandExecutor {

    private final JailPlugin plugin;

    public IPJailCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jails.ipjail")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ip-jail <player> <jail name> [reason] [duration]");
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return false;
        }

        if (plugin.isPlayerJailed(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is already jailed!");
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

        String reason = "No reason provided";
        long duration = -1;

        if (args.length > 2) {
            if (args.length == 3) {
                if (args[2].matches("\\d+[smhd]")) {
                    duration = JailPlugin.parseDuration(args[2]);
                } else {
                    reason = args[2];
                }
            } else if (args.length >= 4) {
                String lastArg = args[args.length - 1];
                if (lastArg.matches("\\d+[smhd]")) {
                    duration = JailPlugin.parseDuration(lastArg);
                    reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length - 1));
                } else {
                    reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                }
            }
        }

        String playerIp = target.getAddress().getAddress().getHostAddress();
        String hashedIp = plugin.hashIpAddress(playerIp);

        plugin.addJailedIp(hashedIp, jailName, duration, reason, sender.getName());

        Location jailLocation = plugin.getJails().get(jailName);
        plugin.jailPlayer(target, jailName, duration, reason, sender.getName());
        plugin.scheduleUnjail(target, duration);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(target) && !plugin.isPlayerJailed(onlinePlayer.getUniqueId())) {
                String onlinePlayerIp = onlinePlayer.getAddress().getAddress().getHostAddress();
                String hashedOnlinePlayerIp = plugin.hashIpAddress(onlinePlayerIp);

                if (hashedIp.equals(hashedOnlinePlayerIp)) {
                    plugin.jailPlayer(onlinePlayer, jailName, duration, "Associated with " + target.getName() + ": " + reason, sender.getName());
                    onlinePlayer.sendMessage(ChatColor.RED + "You have been jailed because your IP is associated with " + target.getName());
                    plugin.scheduleUnjail(onlinePlayer, duration);
                }
            }
        }

        String prefix = plugin.getPrefix();
        String durationText = duration > 0 ? formatDuration(duration) : "permanently";

        String messageTemplate = plugin.getConfig().getString("general.ip-jail-broadcast-message",
                "{prefix} &c{player} has been IP-jailed for {duration} by {jailer}. Reason: {reason}!");

        String broadcastMessage = messageTemplate
                .replace("{prefix}", ChatColor.translateAlternateColorCodes('&', prefix))
                .replace("{player}", target.getName())
                .replace("{duration}", durationText)
                .replace("{jailer}", sender.getName())
                .replace("{reason}", reason);

        if (plugin.getConfig().getBoolean("general.broadcast-on-jail")) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
        }

        target.sendMessage(ChatColor.RED + "You have been IP-jailed " + durationText + " by " + sender.getName() +
                ". Reason: " + reason);

        sender.sendMessage(ChatColor.GREEN + "Player " + target.getName() + " IP-jailed successfully. Any accounts sharing this IP will also be jailed.");

        return true;
    }

    private String formatDuration(long duration) {
        long seconds = duration / 1000;

        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            return (seconds / 60) + " minutes";
        } else if (seconds < 86400) {
            return (seconds / 3600) + " hours";
        } else {
            return (seconds / 86400) + " days";
        }
    }
}