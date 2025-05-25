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
        }

        Player player = (Player) sender;
        plugin.sendJailsPlusMessage(player);


        return true;
    }
}
