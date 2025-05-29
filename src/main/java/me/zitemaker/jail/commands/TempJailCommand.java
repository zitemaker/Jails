package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TempJailCommand implements CommandExecutor {
    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public TempJailCommand(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (args.length < 3) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("tempjail_usage"));
            return false;
        }

        if (!sender.hasPermission("jails.tempjail")) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("tempjail_no_permission"));
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("tempjail_player_not_found"));
            return false;
        }

        String jailName = args[1];
        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("tempjail_jail_not_found"));
            return false;
        }

        long duration = JailPlugin.parseDuration(args[2]);
        if (duration <= 0) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("tempjail_invalid_duration"));
            return false;
        }

        if (plugin.isPlayerJailed(target.getUniqueId())) {
            String msg = String.format(translationManager.getMessage("tempjail_already_jailed"), target.getName());
            sender.sendMessage(prefix + " " + ChatColor.RED + msg);
            return false;
        }

        Player player = (Player) sender;
        plugin.sendJailsPlusMessage(player);
        return true;
    }
}