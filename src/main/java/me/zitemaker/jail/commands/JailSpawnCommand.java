package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.zitemaker.jail.listeners.TranslationManager;
import org.jetbrains.annotations.NotNull;

public class JailSpawnCommand implements CommandExecutor {
    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public JailSpawnCommand(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("jails.spawn")) {
            sender.sendMessage(ChatColor.RED + translationManager.getMessage("jail_spawn_no_permission"));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /jailspawn <player> <world_spawn/original_location>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + translationManager.getMessage("jail_spawn_no_player_found"));
            return true;
        }

        String spawnOption = args[1].toLowerCase();
        if (!spawnOption.equals("world_spawn") && !spawnOption.equals("original_location")) {
            sender.sendMessage(ChatColor.RED + translationManager.getMessage("jail_spawn_invalid"));
            return true;
        }

        Player player = (Player) sender;
        plugin.sendJailsPlusMessage(player);
        return true;
    }
}