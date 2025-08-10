package com.zitemaker.jails.commands.tabcompleter;

import com.zitemaker.jails.JailsFree;
import com.zitemaker.jails.commands.RootCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class TabCompleter implements org.bukkit.command.TabCompleter {
    private final JailsFree plugin;
    private final Set<String> subCommandKeys;

    public TabCompleter(RootCommand rootCommand, JailsFree plugin) {
        this.plugin = plugin;
        this.subCommandKeys = rootCommand.getSubCommands().keySet();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return subCommandKeys.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("jail")) {
            if (args.length == 2) {
                return getPlayerSuggestions(args[1]);
            } else if (args.length == 3) {
                return getJailSuggestions(args[2]);
            } else if (args.length == 4) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("[reason]");
                return suggestions;
            }
            return Collections.emptyList();
        }

        if (subCommand.equals("tempjail")) {
            if (args.length == 2) {
                return getPlayerSuggestions(args[1]);
            } else if (args.length == 3) {
                return getJailSuggestions(args[2]);
            } else if (args.length == 4) {
                return getDurationSuggestions(args[3]);
            }
            return Collections.emptyList();
        }

        if (subCommand.equals("deljail") && args.length == 2) {
            return getJailSuggestions(args[1]);
        }

        if (subCommand.equals("handcuff") && args.length == 2) {
            return getPlayerSuggestions(args[1]);
        }

        if (subCommand.equals("unhandcuff") && args.length == 2) {
            String input = args[1].toLowerCase();
            return plugin.getHandcuffedPlayerNames().stream()
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (subCommand.equals("unjail") && args.length == 2) {
            String input = args[1].toLowerCase();
            return plugin.getJailedPlayerNames().stream()
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (subCommand.equals("setjail") && args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("<jailname>");
            return suggestions;
        }

        if (subCommand.equals("duration") && args.length == 2) {
            String input = args[1].toLowerCase();
            return plugin.getJailedPlayerNames().stream()
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<String> getPlayerSuggestions(String input) {
        List<String> suggestions = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
        if (suggestions.isEmpty()) {
            suggestions.add(input.isEmpty() ? "[offline-player]" : "[offline-player:" + input + "]");
        }
        return suggestions;
    }

    private List<String> getJailSuggestions(String input) {
        Set<String> jailNames = plugin.getJails().keySet();
        return jailNames.stream()
                .filter(jail -> jail.toLowerCase().startsWith(input.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> getDurationSuggestions(String input) {
        List<String> durations = Arrays.asList("30s", "1m", "30m", "60m", "1h", "24h", "1d", "30d");
        return durations.stream()
                .filter(d -> d.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}