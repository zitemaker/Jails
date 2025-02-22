package com.zitemaker.jails.commands;

import com.zitemaker.jails.JailsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JailSetCommand implements CommandExecutor {
    private final JailsPlugin plugin;

    public JailSetCommand(JailsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can set jails!");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /jailset <jail name>");
            return true;
        }

        Player player = (Player) sender;
        Location location = player.getLocation();
        String jailName = args[0].toLowerCase();

        plugin.addJail(jailName, location);
        player.sendMessage(ChatColor.GREEN + "Jail " + ChatColor.YELLOW + jailName + ChatColor.GREEN + " has been set at your current location!");
        return true;
    }
}
