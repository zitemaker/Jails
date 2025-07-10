package com.zitemaker.jails.commands.subcommands;

import com.zitemaker.jails.JailsFree;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import com.zitemaker.jails.interfaces.SubCommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JailDurationSC implements SubCommandExecutor {
    private final JailsFree plugin;

    public JailDurationSC(JailsFree plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSubCommand(@NotNull CommandSender sender, String[] args) {
        String prefix = plugin.getPrefix();

        if (!sender.hasPermission("jails.duration")) {
            sender.sendMessage(plugin.getTranslationManager().getMessage("cr_no_permission").replace("{prefix}", prefix));
            return;
        }

        Player target;

        if (args.length == 0 && sender instanceof Player) {
            target = (Player) sender;
        } else if (args.length == 1) {
            String targetName = args[0];
            target = Bukkit.getPlayer(targetName);
            if (target == null) {
                sender.sendMessage(plugin.getTranslationManager().getMessage("invalid_player").replace("{prefix}", prefix).replace("{player}", targetName));
                return;
            }
        } else {
            sender.sendMessage(ChatColor.RED + plugin.getTranslationManager().getMessage("jailduration_usage").replace("{prefix}", prefix));
            return;
        }

        FileConfiguration config = plugin.getJailedPlayersConfig();
        if (!config.contains(target.getUniqueId().toString())) {
            sender.sendMessage(plugin.getTranslationManager().getMessage("not_jailed").replace("{prefix}", prefix).replace("{player}", target.getName()));
            return;
        }

        long endTime = config.getLong(target.getUniqueId() + ".endTime");
        if (endTime == -1) {
            sender.sendMessage(plugin.getTranslationManager().getMessage("permanently_jailed").replace("{prefix}", prefix).replace("{player}", target.getName()));
            return;
        }
        if (System.currentTimeMillis() > endTime) {
            sender.sendMessage(plugin.getTranslationManager().getMessage("jail_time_expired").replace("{prefix}", prefix).replace("{player}", target.getName()));
            return;
        }

        long remainingTime = (endTime - System.currentTimeMillis());
        String formattedTime = plugin.formatTimeLeft(remainingTime);
        sender.sendMessage(plugin.getTranslationManager().getMessage("temporarily_jailed").replace("{prefix}", prefix).replace("{player}", target.getName()).replace("{time}", formattedTime));
    }
}