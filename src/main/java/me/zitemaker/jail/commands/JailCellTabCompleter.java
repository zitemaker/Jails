package me.zitemaker.jail.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JailCellTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("cell");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("cell")) {
            completions.add("spawnset");
        }

        return completions;
    }
}