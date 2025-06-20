package com.zitemaker.jail.commands;

import com.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JailDurationCommand implements CommandExecutor {
    private final JailPlugin plugin;

    public JailDurationCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String prefix = plugin.getPrefix();
        Player target;

        if (args.length == 0 && sender instanceof Player) {
            target = (Player) sender;
        } else if (args.length == 1) {
            String targetName = args[0];
            target = Bukkit.getPlayer(targetName);
            if (target == null) {
                sender.sendMessage(plugin.getTranslationManager().getMessage("invalid_player").replace("{prefix}", prefix).replace("{player}", targetName));
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.RED + plugin.getTranslationManager().getMessage("jailduration_usage").replace("{prefix}", prefix));
            return true;
        }

        FileConfiguration config = plugin.getJailedPlayersConfig();
        if (!config.contains(target.getUniqueId().toString())) {
            sender.sendMessage(plugin.getTranslationManager().getMessage("not_jailed").replace("{prefix}", prefix).replace("{player}", target.getName()));
            return true;
        }

        long endTime = config.getLong(target.getUniqueId() + ".endTime");
        if (endTime == -1) {
            sender.sendMessage(plugin.getTranslationManager().getMessage("permanently_jailed").replace("{prefix}", prefix).replace("{player}", target.getName()));
            return true;
        }
        if (System.currentTimeMillis() > endTime) {
            sender.sendMessage(plugin.getTranslationManager().getMessage("jail_time_expired").replace("{prefix}", prefix).replace("{player}", target.getName()));
            return true;
        }

        long remainingTime = (endTime - System.currentTimeMillis());
        String formattedTime = plugin.formatTimeLeft(remainingTime);
        sender.sendMessage(plugin.getTranslationManager().getMessage("temporarily_jailed").replace("{prefix}", prefix).replace("{player}", target.getName()).replace("{time}", formattedTime));
        return true;
    }
}