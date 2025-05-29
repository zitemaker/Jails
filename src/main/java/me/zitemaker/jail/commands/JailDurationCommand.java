package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JailDurationCommand implements CommandExecutor {
    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public JailDurationCommand(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("jailduration_only_players"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("jails.jailduration")) {
            player.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("jailduration_no_permission"));
            return true;
        }

        plugin.sendJailsPlusMessage(player);

        return true;
    }
}