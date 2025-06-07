package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TempJailTabCompleter implements TabCompleter {

    private final JailPlugin plugin;
    private static final List<String> DURATIONS = Arrays.asList("10s", "30s", "1m", "5m", "10m", "30m", "1h", "2h", "6h", "12h", "1d", "3d", "7d");

    public TempJailTabCompleter(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
        } else if (args.length == 2) {
            suggestions.addAll(plugin.getJails().keySet());
        } else if (args.length == 3) {
            suggestions.addAll(DURATIONS);
        } else if (args.length == 4) {
            suggestions.add("[reason]");
        }

        return suggestions;
    }
}