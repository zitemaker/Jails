package me.zitemaker.jail.commands;

import me.zitemaker.jail.utils.JailUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JailTabCompleter implements TabCompleter {
    private final JailUtils jailUtils;

    public JailTabCompleter(JailUtils jailUtils) {
        this.jailUtils = jailUtils;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest online player names
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
        } else if (args.length == 2) {
            // Suggest jail cell names
            completions.addAll(jailUtils.getAllJailCells());
        } else if (args.length == 3) {
            // Suggest duration formats
            completions.addAll(Arrays.asList("10s", "1m", "1h", "1d", "1y"));
        }

        return completions;
    }
}