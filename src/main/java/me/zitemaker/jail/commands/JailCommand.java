package me.zitemaker.jail.commands;

import me.zitemaker.jail.utils.JailUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JailCommand implements CommandExecutor {
    private final JailUtils jailUtils;

    public JailCommand(JailUtils jailUtils) {
        this.jailUtils = jailUtils;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /jail <player> <jail-name> <duration> [reason]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        String jailName = args.length > 1 ? args[1] : jailUtils.getDefaultJail();
        Location jailSpawn = jailUtils.getJailCellLocation(jailName);
        if (jailSpawn == null) {
            sender.sendMessage("Jail '" + jailName + "' not found.");
            return true;
        }

        String duration = args[2];
        String reason = args.length > 3 ? args[3] : "No reason provided";

        // Teleport player to jail and set timer
        target.teleport(jailSpawn);
        sender.sendMessage(target.getName() + " has been jailed in '" + jailName + "' for " + duration + ". Reason: " + reason);

        // Schedule release logic (to be implemented)

        return true;
    }
}