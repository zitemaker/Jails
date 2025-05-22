package me.zitemaker.jail.ipjail;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
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

    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public IPJailCommand(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (!validateCommand(sender, args)) {
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(prefix + ChatColor.RED + translationManager.getMessage("ipjail_player_not_found"));
            return true;
        }

        if (plugin.isPlayerJailed(target.getUniqueId())) {
            String msg = String.format(translationManager.getMessage("ipjail_already_jailed"), target.getName());
            sender.sendMessage(prefix + ChatColor.RED + msg);
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
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + translationManager.getMessage("ipjail_no_permission"));
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + translationManager.getMessage("ipjail_usage"));
            return false;
        }
        return true;
    }

    private boolean validateJailExists(CommandSender sender, String jailName) {
        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + translationManager.getMessage("ipjail_jail_not_found"));
            plugin.getJails().forEach((name, location) ->
                    sender.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "- " + name));
            return false;
        }
        return true;
    }

    private CommandParameters parseCommandParameters(String[] args) {
        String reason = translationManager.getMessage("ipjail_default_reason");
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
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + translationManager.getMessage("ipjail_ip_processing_failed"));
                return null;
            }
            return hashedIp;
        } catch (Exception e) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + translationManager.getMessage("ipjail_ip_processing_error"));
            plugin.getLogger().warning("Error processing IP address for " + target.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private void processJailing(CommandSender sender, Player target, String jailName, String hashedIp, CommandParameters params) {
        long endTime = params.duration > 0 ? System.currentTimeMillis() + params.duration : -1;

        jailPlayerAndBroadcast(sender, target, jailName, hashedIp, params, endTime);

        jailMatchingIpPlayers(sender, target, jailName, hashedIp, params, endTime);

        String msg = String.format(translationManager.getMessage("ipjail_success"), target.getName());
        sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + msg);
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
        String notificationMsg = String.format(translationManager.getMessage("ipjail_notification"), durationText, sender.getName(), params.reason);
        player.sendMessage(ChatColor.RED + notificationMsg);

        broadcastJailMessage(sender, player, params, durationText);
    }

    private void jailMatchingIpPlayers(CommandSender sender, Player primaryTarget, String jailName,
                                       String hashedIp, CommandParameters params, long endTime) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (shouldJailMatchingPlayer(onlinePlayer, primaryTarget)) {
                String onlinePlayerIp = onlinePlayer.getAddress().getAddress().getHostAddress();
                String hashedOnlinePlayerIp = plugin.hashIpAddress(onlinePlayerIp);

                if (hashedIp.equals(hashedOnlinePlayerIp)) {
                    String associatedReason = String.format(translationManager.getMessage("ipjail_associated_reason"), primaryTarget.getName(), params.reason);
                    plugin.jailPlayer(onlinePlayer, jailName, endTime, associatedReason, sender.getName());
                    String associatedMsg = String.format(translationManager.getMessage("ipjail_associated_notification"), primaryTarget.getName());
                    onlinePlayer.sendMessage(ChatColor.RED + associatedMsg);

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

        String broadcastTemplate = translationManager.getMessage("ipjail_broadcast");
        String broadcastMessage = broadcastTemplate
                .replace("{prefix}", plugin.getPrefix())
                .replace("{player}", player.getName())
                .replace("{duration}", durationText)
                .replace("{jailer}", sender.getName())
                .replace("{reason}", params.reason);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
    }

    private String formatDuration(long durationMillis) {
        if (durationMillis <= 0) {
            return translationManager.getMessage("ipjail_permanent_duration");
        }

        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis);

        if (seconds < 60) {
            return String.format(translationManager.getMessage("ipjail_seconds"), seconds);
        }

        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        if (minutes < 60) {
            return String.format(translationManager.getMessage("ipjail_minutes"), minutes);
        }

        long hours = TimeUnit.MINUTES.toHours(minutes);
        if (hours < 24) {
            return String.format(translationManager.getMessage("ipjail_hours"), hours);
        }

        long days = TimeUnit.HOURS.toDays(hours);
        return String.format(translationManager.getMessage("ipjail_days"), days);
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