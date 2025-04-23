package me.zitemaker.jail.ipjail;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IPJailTabCompleter implements TabCompleter {
    private final JailPlugin plugin;

    public IPJailTabCompleter(JailPlugin plugin) {
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
            completions.addAll(Arrays.asList("10s", "30s", "1m", "5m", "10m", "1h", "3h", "6h", "12h", "1d", "7d", "30d"));
        }

        return completions;
    }
}