package com.zitemaker.jails.commands;

import com.zitemaker.jails.JailsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DelJailTabCompleter implements TabCompleter {

    private final JailsPlugin plugin;

    public DelJailTabCompleter(JailsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return plugin.getJails().keySet().stream()
                    .filter(jail -> jail.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}