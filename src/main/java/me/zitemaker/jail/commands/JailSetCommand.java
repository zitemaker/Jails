package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class JailSetCommand implements CommandExecutor, TabCompleter {
    private final JailPlugin plugin;

    public JailSetCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can set jails!");
            return true;
        }

        if(!sender.hasPermission("jails.setjail")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return false;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /setjail <jail name>");
            return true;
        }

        Player player = (Player) sender;
        Location location = player.getLocation();
        String jailName = args[0].toLowerCase();

        plugin.addJail(jailName, location);
        player.sendMessage(ChatColor.GREEN + "Jail " + ChatColor.YELLOW + jailName + ChatColor.GREEN + " has been set at your current location!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("jails.setjail")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("<jailname>");
            return suggestions;
        }

        return new ArrayList<>();
    }
}