package com.zitemaker.jail.commands;

import com.zitemaker.jail.JailPlugin;
import com.zitemaker.jail.translation.TranslationManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class JailSetCommand implements CommandExecutor, TabCompleter {
    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public JailSetCommand(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(translationManager.getMessage("setjail_only_players").replace("{prefix}", plugin.getPrefix()));
            return true;
        }

        if (!sender.hasPermission("jails.setjail")) {
            sender.sendMessage(translationManager.getMessage("setjail_no_permission").replace("{prefix}", plugin.getPrefix()));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(translationManager.getMessage("setjail_usage").replace("{prefix}", plugin.getPrefix()));
            return true;
        }

        Location location = player.getLocation();
        String jailName = args[0].toLowerCase();

        plugin.addJail(jailName, location);
        String successMsg = String.format(translationManager.getMessage("setjail_success").replace("{prefix}", plugin.getPrefix()), jailName);
        player.sendMessage(ChatColor.GREEN + successMsg);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!sender.hasPermission("jails.setjail")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("<jailname>");
            return suggestions;
        }

        return new ArrayList<>();
    }
}