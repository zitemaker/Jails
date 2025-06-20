package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class TempJailCommand implements CommandExecutor {
    private final JailPlugin plugin;

    public TempJailCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (args.length < 3) {
            sender.sendMessage(prefix + " " + ChatColor.RED + "Usage: /tempjail <player> <jail name> <duration (e.g., 2d, 3h)> [reason]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        String targetName = args[0];
        if (target == null) {
            sender.sendMessage(prefix + " " + ChatColor.RED +
                    plugin.getTranslationManager().getMessage("invalid_player").replace("{player}", targetName));
            return true;
        }

        String jailName = args[1];
        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(prefix + " " + ChatColor.RED +
                    plugin.getTranslationManager().getMessage("invalid_jail").replace("{jail}", jailName));
            return true;
        }

        long duration = JailPlugin.parseDuration(args[2]);
        if (duration <= 0) {
            sender.sendMessage(prefix + " " + ChatColor.RED +
                    plugin.getTranslationManager().getMessage("tempjail_invalid_duration"));
            return true;
        }

        if(plugin.isPlayerJailed(target.getUniqueId())){
            sender.sendMessage(prefix + " " + ChatColor.RED +
                    plugin.getTranslationManager().getMessage("already_jailed").replace("{player}", target.getName()));
            return true;
        }

        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "No reason provided";
        long endTime = System.currentTimeMillis() + duration;
        String formattedTime = plugin.formatTimeLeft(duration);

        plugin.jailPlayer(target, jailName, endTime, reason, sender.getName());
        plugin.scheduleUnjail(target, duration);

        target.sendMessage(prefix + " " + ChatColor.YELLOW +
                plugin.getTranslationManager().getMessage("tempjail_success_target")
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

        if(plugin.getConfig().getBoolean("general.broadcast-on-jail")){
            Bukkit.broadcastMessage(ChatColor.GREEN + " " + ChatColor.translateAlternateColorCodes('&', prefix + " " + broadcastMessage));
        } else {
            sender.sendMessage(prefix + " " + ChatColor.GREEN +
                    plugin.getTranslationManager().getMessage("tempjail_success_jailer")
                            .replace("{player}", target.getName())
                            .replace("{duration}", formattedTime));
        }

        return true;
    }
}