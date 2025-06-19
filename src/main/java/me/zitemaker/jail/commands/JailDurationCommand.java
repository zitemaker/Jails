package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class JailDurationCommand implements CommandExecutor {
    private final JailPlugin plugin;

    public JailDurationCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();
        Player target;

        if (args.length == 0 && sender instanceof Player) {
            target = (Player) sender;
        } else if (args.length == 1) {
            String targetName = args[0];
            target = Bukkit.getPlayer(targetName);
            if (target == null) {
                sender.sendMessage(prefix + " " + ChatColor.RED +
                        plugin.getTranslationManager().getMessage("invalid_player").replace("{player}", targetName));
                return true;
            }
        } else {
            sender.sendMessage(prefix + " " + ChatColor.RED + "Usage: /jailduration [player]");
            return true;
        }

        FileConfiguration config = plugin.getJailedPlayersConfig();
        if (!config.contains(target.getUniqueId().toString())) {
            sender.sendMessage(prefix + " " + ChatColor.RED +
                    plugin.getTranslationManager().getMessage("not_jailed").replace("{player}", target.getName()));
            return true;
        }

        long endTime = config.getLong(target.getUniqueId() + ".endTime");
        if (endTime == -1) {
            sender.sendMessage(prefix + " " + ChatColor.YELLOW +
                    plugin.getTranslationManager().getMessage("permanently_jailed").replace("{player}", target.getName()));
            return true;
        }
        if (System.currentTimeMillis() > endTime) {
            sender.sendMessage(prefix + " " + ChatColor.RED +
                    plugin.getTranslationManager().getMessage("jail_time_jailed").replace("{player}", target.getName()));
            return true;
        }

        long remainingTime = (endTime - System.currentTimeMillis());
        String formattedTime = plugin.formatTimeLeft(remainingTime);
        sender.sendMessage(prefix + " " + ChatColor.GREEN +
                plugin.getTranslationManager().getMessage("temporarily_jailed")
                        .replace("{player}", target.getName())
                        .replace("{time}", formattedTime));
        return true;
    }
}