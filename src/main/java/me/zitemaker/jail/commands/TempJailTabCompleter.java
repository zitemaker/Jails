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

    public TempJailTabCompleter(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
        } else if (args.length == 2) {
            completions.addAll(plugin.getJails().keySet());
        } else if (args.length == 3) {
            completions.addAll(Arrays.asList("10s", "10m", "10h", "10d"));
        }

        return completions;
    }
}