package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
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

    public JailCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jails.jail")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /jail <player> <jail name> [reason]");
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return false;
        }

        if(plugin.isPlayerJailed(target.getUniqueId())){
            sender.sendMessage(ChatColor.RED + target.getName() + " is already jailed!");
            return false;
        }


        String jailName = args[1];
        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(ChatColor.RED + "Jail not found. Available jails:");
            plugin.getJails().forEach((name, location) ->
                    sender.sendMessage(ChatColor.GOLD + "- " + name)
            );
            return false;
        }

        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "No reason provided";

        Location jailLocation = plugin.getJails().get(jailName);
        plugin.jailPlayer(target, jailName, -1, reason, sender.getName());

        String prefix = plugin.getPrefix();

        String messageTemplate = plugin.getConfig().getString("general.jail-broadcast-message",
                "{prefix} &c{player} has been jailed permanently by {jailer}. Reason: {reason}!");


        String broadcastMessage = messageTemplate
                .replace("{prefix}", ChatColor.translateAlternateColorCodes('&', prefix))
                .replace("{player}", target.getName())
                .replace("{duration}", "a permanent duration")
                .replace("{jailer}", sender.getName())
                .replace("{reason}", reason);



        if(plugin.getConfig().getBoolean("general.broadcast-on-jail")){
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
        }

        target.sendMessage(ChatColor.RED + "You have been jailed permanently by " + sender.getName() +
                ". Reason: " + reason);
        return true;
    }
}