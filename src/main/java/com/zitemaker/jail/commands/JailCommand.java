package com.zitemaker.jail.commands;

import com.zitemaker.jail.JailPlugin;
import com.zitemaker.jail.translation.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class JailCommand implements CommandExecutor {

    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public JailCommand(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("jails.jail")) {
            sender.sendMessage(translationManager.getMessage("jail_no_permission").replace("{prefix}", plugin.getPrefix()));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(translationManager.getMessage("jail_usage").replace("{prefix}", plugin.getPrefix()));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(translationManager.getMessage("jail_player_not_found").replace("{prefix}", plugin.getPrefix()));
            return true;
        }

        if (plugin.isPlayerJailed(target.getUniqueId())) {
            String msg = String.format(translationManager.getMessage("jail_already_jailed").replace("{prefix}", plugin.getPrefix()), target.getName());
            sender.sendMessage(ChatColor.RED + msg);
            return true;
        }

        String jailName = args[1];
        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(translationManager.getMessage("jail_not_found").replace("{prefix}", plugin.getPrefix()));
            plugin.getJails().forEach((name, location) ->
                    sender.sendMessage(ChatColor.GOLD + "- " + name)
            );
            return true;
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

        if (plugin.getConfig().getBoolean("general.broadcast-on-jail")) {
            Bukkit.broadcastMessage(ChatColor.GREEN + " " + ChatColor.translateAlternateColorCodes('&', broadcastMessage));
        }

        String notificationMsg = String.format(translationManager.getMessage("jail_notification").replace("{prefix}", plugin.getPrefix()), sender.getName(), reason);
        target.sendMessage(ChatColor.RED + notificationMsg);
        return true;
    }
}