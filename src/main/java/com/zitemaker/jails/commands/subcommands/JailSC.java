package com.zitemaker.jails.commands.subcommands;

import com.zitemaker.jails.JailsFree;
import com.zitemaker.jails.interfaces.SubCommandExecutor;
import com.zitemaker.jails.translation.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class JailSC implements SubCommandExecutor {

    private final JailsFree plugin;
    private final TranslationManager translationManager;

    public JailSC(JailsFree plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public void onSubCommand(@NotNull CommandSender sender, String[] args) {
        if (!sender.hasPermission("jails.jails")) {
            sender.sendMessage(translationManager.getMessage("jail_no_permission").replace("{prefix}", plugin.getPrefix()));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(translationManager.getMessage("jail_usage").replace("{prefix}", plugin.getPrefix()));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(translationManager.getMessage("jail_player_not_found").replace("{prefix}", plugin.getPrefix()));
            return;
        }

        if (plugin.isPlayerJailed(target.getUniqueId())) {
            String msg = String.format(translationManager.getMessage("jail_already_jailed").replace("{prefix}", plugin.getPrefix()), target.getName());
            sender.sendMessage(ChatColor.RED + msg);
            return;
        }

        String jailName = args[1];
        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(translationManager.getMessage("jail_not_found").replace("{prefix}", plugin.getPrefix()));

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

        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : translationManager.getMessage("jail_default_reason");

        Location jailLocation = plugin.getJails().get(jailName);
        plugin.jailPlayer(target, jailName, -1, reason, sender.getName());

        String broadcastTemplate = translationManager.getMessage("jail_broadcast");
        String broadcastMessage = broadcastTemplate
                .replace("{prefix}", plugin.getPrefix())
                .replace("{player}", target.getName())
                .replace("{duration}", translationManager.getMessage("jail_permanent_duration").replace("{prefix}", plugin.getPrefix()))
                .replace("{jailer}", sender.getName())
                .replace("{reason}", reason);

        if (plugin.getConfig().getBoolean("general.broadcast-on-jails")) {
            Bukkit.broadcastMessage(ChatColor.GREEN + " " + ChatColor.translateAlternateColorCodes('&', broadcastMessage));
        }

        String notificationMsg = String.format(translationManager.getMessage("jail_notification").replace("{prefix}", plugin.getPrefix()), sender.getName(), reason);
        target.sendMessage(ChatColor.RED + notificationMsg);
    }
}