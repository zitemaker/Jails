package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("setjail_only_players"));
            return true;
        }

        if (!sender.hasPermission("jails.setjail")) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("setjail_no_permission"));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("setjail_usage"));
            return true;
        }

        Player player = (Player) sender;
        Location location = player.getLocation();
        String jailName = args[0].toLowerCase();

        plugin.addJail(jailName, location);
        String successMsg = String.format(translationManager.getMessage("setjail_success"), jailName);
        player.sendMessage(prefix + " " + ChatColor.GREEN + successMsg);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
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