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
import java.util.concurrent.TimeUnit;

public class IPJailCommand implements CommandExecutor {


    private static final String PERMISSION = "jails.ipjail";
    private static final String USAGE_MESSAGE = ChatColor.RED + "Usage: /ip-jail <player> <jail name> [reason] [duration]";
    private static final String DEFAULT_REASON = "No reason provided";

    private final JailPlugin plugin;

    public IPJailCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!validateCommand(sender, args)) {
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        if (plugin.isPlayerJailed(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is already jailed!");
            return true;
        }

        String jailName = args[1];
        if (!validateJailExists(sender, jailName)) {
            return true;
        }

        CommandParameters params = parseCommandParameters(args);
        String hashedIp = getHashedIp(sender, target);
        if (hashedIp == null) {
            return true;
        }

        processJailing(sender, target, jailName, hashedIp, params);
        return true;
    }

    private boolean validateCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(USAGE_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean validateJailExists(CommandSender sender, String jailName) {
        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(ChatColor.RED + "Jail not found. Available jails:");
            plugin.getJails().forEach((name, location) ->
                    sender.sendMessage(ChatColor.GOLD + "- " + name));
            return false;
        }
        return true;
    }

    private CommandParameters parseCommandParameters(String[] args) {
        String reason = DEFAULT_REASON;
        long duration = -1;

        if (args.length > 2) {
            for (int i = args.length - 1; i >= 2; i--) {
                if (args[i].matches("\\d+[smhd]")) {
                    duration = JailPlugin.parseDuration(args[i]);
                    reason = String.join(" ", Arrays.copyOfRange(args, 2, i));
                    break;
                }
            }

            if (duration == -1) {
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
        }

        return new CommandParameters(reason, duration);
    }

    private String getHashedIp(CommandSender sender, Player target) {
        try {
            String playerIp = target.getAddress().getAddress().getHostAddress();
            String hashedIp = plugin.hashIpAddress(playerIp);
            if (hashedIp == null) {
                sender.sendMessage(ChatColor.RED + "Failed to process IP address.");
                return null;
            }
            return hashedIp;
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error processing IP address.");
            plugin.getLogger().warning("Error processing IP address for " + target.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private void processJailing(CommandSender sender, Player target, String jailName, String hashedIp, CommandParameters params) {
        long endTime = params.duration > 0 ? System.currentTimeMillis() + params.duration : -1;

        jailPlayerAndBroadcast(sender, target, jailName, hashedIp, params, endTime);

        jailMatchingIpPlayers(sender, target, jailName, hashedIp, params, endTime);

        sender.sendMessage(ChatColor.GREEN + "Player " + target.getName() +
                " IP-jailed successfully. Any accounts sharing this IP will also be jailed.");
    }

    private void jailPlayerAndBroadcast(CommandSender sender, Player player, String jailName,
                                        String hashedIp, CommandParameters params, long endTime) {
        plugin.addJailedIp(hashedIp, jailName, params.duration, params.reason, sender.getName());

        Location jailLocation = plugin.getJails().get(jailName);
        plugin.jailPlayer(player, jailName, endTime, params.reason, sender.getName());

        if (params.duration > 0) {
            plugin.scheduleUnjail(player, params.duration);
        }

        String durationText = formatDuration(params.duration);
        player.sendMessage(ChatColor.RED + "You have been IP-jailed " + durationText +
                " by " + sender.getName() + ". Reason: " + params.reason);

        broadcastJailMessage(sender, player, params, durationText);
    }

    private void jailMatchingIpPlayers(CommandSender sender, Player primaryTarget, String jailName,
                                       String hashedIp, CommandParameters params, long endTime) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (shouldJailMatchingPlayer(onlinePlayer, primaryTarget)) {
                String onlinePlayerIp = onlinePlayer.getAddress().getAddress().getHostAddress();
                String hashedOnlinePlayerIp = plugin.hashIpAddress(onlinePlayerIp);

                if (hashedIp.equals(hashedOnlinePlayerIp)) {
                    String associatedReason = "Associated with " + primaryTarget.getName() + ": " + params.reason;
                    plugin.jailPlayer(onlinePlayer, jailName, endTime, associatedReason, sender.getName());
                    onlinePlayer.sendMessage(ChatColor.RED + "You have been jailed because your IP is associated with " + primaryTarget.getName());

                    if (params.duration > 0) {
                        plugin.scheduleUnjail(onlinePlayer, params.duration);
                    }
                }
            }
        }
    }

    private boolean shouldJailMatchingPlayer(Player player, Player primaryTarget) {
        return !player.equals(primaryTarget) && !plugin.isPlayerJailed(player.getUniqueId());
    }

    private void broadcastJailMessage(CommandSender sender, Player player,
                                      CommandParameters params, String durationText) {
        if (!plugin.getConfig().getBoolean("general.broadcast-on-jail", true)) {
            return;
        }

        String prefix = plugin.getPrefix();
        String messageTemplate = plugin.getConfig().getString("general.ip-jail-broadcast-message",
                "{prefix} &c{player} has been IP-jailed for {duration} by {jailer}. Reason: {reason}!");

        String broadcastMessage = messageTemplate
                .replace("{prefix}", ChatColor.translateAlternateColorCodes('&', prefix))
                .replace("{player}", player.getName())
                .replace("{duration}", durationText)
                .replace("{jailer}", sender.getName())
                .replace("{reason}", params.reason);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
    }

    private String formatDuration(long durationMillis) {
        if (durationMillis <= 0) {
            return "permanently";
        }

        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis);

        if (seconds < 60) {
            return seconds + " second" + (seconds != 1 ? "s" : "");
        }

        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        if (minutes < 60) {
            return minutes + " minute" + (minutes != 1 ? "s" : "");
        }

        long hours = TimeUnit.MINUTES.toHours(minutes);
        if (hours < 24) {
            return hours + " hour" + (hours != 1 ? "s" : "");
        }

        long days = TimeUnit.HOURS.toDays(hours);
        return days + " day" + (days != 1 ? "s" : "");
    }

    private static class CommandParameters {
        final String reason;
        final long duration;

        CommandParameters(String reason, long duration) {
            this.reason = reason;
            this.duration = duration;
        }
    }
}