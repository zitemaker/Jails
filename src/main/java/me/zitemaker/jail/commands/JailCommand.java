package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class JailCommand implements CommandExecutor {

    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public JailCommand(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (!sender.hasPermission("jails.jail")) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("jail_no_permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("jail_usage"));
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("jail_player_not_found"));
            return false;
        }

        if (plugin.isPlayerJailed(target.getUniqueId())) {
            String msg = String.format(translationManager.getMessage("jail_already_jailed"), target.getName());
            sender.sendMessage(prefix + " " + ChatColor.RED + msg);
            return false;
        }

        String jailName = args[1];
        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("jail_not_found"));
            plugin.getJails().forEach((name, location) ->
                    sender.sendMessage(prefix + " " + ChatColor.GOLD + "- " + name)
            );
            return false;
        }

        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : translationManager.getMessage("jail_default_reason");

        Location jailLocation = plugin.getJails().get(jailName);
        plugin.jailPlayer(target, jailName, -1, reason, sender.getName());

        String broadcastTemplate = translationManager.getMessage("jail_broadcast");
        String broadcastMessage = broadcastTemplate
                .replace("{prefix}", prefix)
                .replace("{player}", target.getName())
                .replace("{duration}", translationManager.getMessage("jail_permanent_duration"))
                .replace("{jailer}", sender.getName())
                .replace("{reason}", reason);

        if (plugin.getConfig().getBoolean("general.broadcast-on-jail")) {
            Bukkit.broadcastMessage(ChatColor.GREEN + " " + ChatColor.translateAlternateColorCodes('&', broadcastMessage));
        }

        String notificationMsg = String.format(translationManager.getMessage("jail_notification"), sender.getName(), reason);
        target.sendMessage(prefix + " " + ChatColor.RED + notificationMsg);
        return true;
    }
}