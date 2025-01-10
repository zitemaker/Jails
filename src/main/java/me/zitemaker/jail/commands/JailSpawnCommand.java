package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JailSpawnCommand implements CommandExecutor {

    private final JailPlugin plugin;

    public JailSpawnCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jailplugin.jailspawn")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /jailspawn <player> <world_spawn/original_location>");
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        String spawnOption = args[1].toLowerCase();
        if (!spawnOption.equals("world_spawn") && !spawnOption.equals("original_location")) {
            sender.sendMessage(ChatColor.RED + "Invalid spawn option. Use 'world_spawn' or 'original_location'.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        plugin.setPlayerSpawnOption(targetUUID, spawnOption);

        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s spawn to " + spawnOption + ".");
        return true;
    }
}