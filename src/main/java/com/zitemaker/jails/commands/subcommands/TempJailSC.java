package com.zitemaker.jails.commands.subcommands;

import com.zitemaker.jails.JailsFree;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import com.zitemaker.jails.interfaces.SubCommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class TempJailSC implements SubCommandExecutor {
    private final JailsFree plugin;

    public TempJailSC(JailsFree plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSubCommand(@NotNull CommandSender sender, String[] args) {
        String prefix = plugin.getPrefix();

        if (!sender.hasPermission("jails.tempjail")) {
            sender.sendMessage(plugin.getTranslationManager().getMessage("cr_no_permission").replace("{prefix}", prefix));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(prefix + " " + ChatColor.RED + "Usage: /tempjail <player> <jails name> <duration (e.g., 2d, 3h)> [reason]");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        String targetName = args[0];
        if (target == null) {
            sender.sendMessage(prefix + " " + ChatColor.RED +
                    plugin.getTranslationManager().getMessage("invalid_player").replace("{player}", targetName));
            return;
        }

        String jailName = args[1];
        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(prefix + " " + ChatColor.RED +
                    plugin.getTranslationManager().getMessage("invalid_jail").replace("{jails}", jailName));

            String suggestion = com.zitemaker.jails.utils.Helpers.findClosestMatch(jailName, plugin.getJails().keySet(), 3);
            if (suggestion != null) {
                sender.sendMessage(ChatColor.YELLOW + "Did you mean '" + ChatColor.GOLD + suggestion + ChatColor.YELLOW + "'?");
            }

            sender.sendMessage(ChatColor.YELLOW + "Available jails:");
            plugin.getJails().forEach((name, location) ->
                    sender.sendMessage(ChatColor.GOLD + "- " + name)
            );
            return;
        }

        long duration = JailsFree.parseDuration(args[2]);
        if (duration <= 0) {
            sender.sendMessage(prefix + " " + ChatColor.RED +
                    plugin.getTranslationManager().getMessage("tempjail_invalid_duration"));
            return;
        }

        if(plugin.isPlayerJailed(target.getUniqueId())){
            sender.sendMessage(prefix + " " + ChatColor.RED +
                    plugin.getTranslationManager().getMessage("already_jailed").replace("{player}", target.getName()));
            return;
        }

        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "No reason provided";
        long endTime = System.currentTimeMillis() + duration;
        String formattedTime = plugin.formatTimeLeft(duration);

        plugin.jailPlayer(target, jailName, endTime, reason, sender.getName());
        plugin.scheduleUnjail(target, duration);

        target.sendMessage(plugin.getTranslationManager().getMessage("tempjail_success_target")
                .replace("{prefix}", prefix)
                .replace("{duration}", formattedTime)
                .replace("{jailer}", sender.getName())
                .replace("{reason}", reason));

        String messageTemplate = plugin.getTranslationManager().getMessage("jail_broadcast_message");
        String broadcastMessage = messageTemplate
                .replace("{prefix}", prefix)
                .replace("{player}", target.getName())
                .replace("{duration}", formattedTime)
                .replace("{jailer}", sender.getName())
                .replace("{reason}", reason);

        if(plugin.getConfig().getBoolean("general.broadcast-on-jails")){
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
        } else {
            sender.sendMessage(plugin.getTranslationManager().getMessage("tempjail_success_jailer")
                    .replace("{prefix}", prefix)
                    .replace("{player}", target.getName())
                    .replace("{duration}", formattedTime));
        }

    }
}