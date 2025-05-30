package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import me.zitemaker.jail.listeners.TranslationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class JailListCommand implements CommandExecutor, TabCompleter, Listener {
    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public JailListCommand(JailPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + translationManager.getMessage("jailed_list_only_players"));
            return true;
        }

        if (!player.hasPermission("jails.list")) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("jailed_list_no_permission"));
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatColor.RED + "Usage: /jailed list");
            return true;
        }

        plugin.sendJailsPlusMessage(player);
        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("list");
        }
        return Collections.emptyList();
    }
}