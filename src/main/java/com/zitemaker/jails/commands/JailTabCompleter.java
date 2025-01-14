package com.zitemaker.jails.commands;

import com.zitemaker.jails.JailsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class JailTabCompleter implements TabCompleter {
    private final JailsPlugin plugin;

    public JailTabCompleter(JailsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) { // Suggest online player names
            Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
        } else if (args.length == 2) { // Suggest jail names
            suggestions.addAll(plugin.getJails().keySet());
        }
        return suggestions;
    }
}
