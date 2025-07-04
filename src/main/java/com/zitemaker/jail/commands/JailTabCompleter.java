package com.zitemaker.jail.commands;

import com.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class JailTabCompleter implements TabCompleter {
    private final JailPlugin plugin;

    public JailTabCompleter(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
        } else if (args.length == 2) {
            suggestions.addAll(plugin.getJails().keySet());
        }
        return suggestions;
    }
}
