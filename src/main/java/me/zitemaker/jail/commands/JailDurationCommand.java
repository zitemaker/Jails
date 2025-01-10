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
        Player target;
        if (args.length == 0 && sender instanceof Player) {
            target = (Player) sender;
        } else if (args.length == 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return false;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /jailduration [player]");
            return false;
        }

        FileConfiguration config = plugin.getJailedPlayersConfig();
        if (!config.contains(target.getUniqueId().toString())) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is not jailed.");
            return true;
        }

        long endTime = config.getLong(target.getUniqueId() + ".endTime");
        if (System.currentTimeMillis() > endTime) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is no longer jailed.");
            return true;
        }

        long remainingTime = (endTime - System.currentTimeMillis()) / 1000;
        sender.sendMessage(ChatColor.GREEN + target.getName() + " has " + remainingTime + " seconds remaining in jail.");
        return true;
    }
}
