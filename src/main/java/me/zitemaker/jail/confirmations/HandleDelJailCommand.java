package me.zitemaker.jail.confirmations;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.commands.DelJailCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HandleDelJailCommand implements CommandExecutor {
    private final JailPlugin plugin;
    private final DelJailCommand delJailCommand;

    public HandleDelJailCommand(JailPlugin plugin, DelJailCommand delJailCommand) {
        this.plugin = plugin;
        this.delJailCommand = delJailCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1 || (!args[0].equalsIgnoreCase("yes") && !args[0].equalsIgnoreCase("no"))) {
            player.sendMessage(ChatColor.RED + "Usage: " + ChatColor.YELLOW + "/handledeljail <yes|no>");
            return true;
        }

        String action = args[0].toLowerCase();
        UUID playerUUID = player.getUniqueId();
        String jailName = delJailCommand.getPendingDeletions().get(playerUUID);

        if (jailName == null) {
            player.sendMessage(ChatColor.RED + "No pending jail deletion found.");
            return true;
        }

        if (action.equals("yes")) {
            if (!plugin.getJails().containsKey(jailName)) {
                player.sendMessage(ChatColor.RED + "Jail '" + ChatColor.YELLOW + jailName + ChatColor.RED + "' no longer exists.");
            } else {
                plugin.removeJail(jailName);
                player.sendMessage(ChatColor.GREEN + "Jail " + ChatColor.YELLOW + jailName + ChatColor.GREEN + " has been successfully deleted.");
            }
        } else {
            player.sendMessage(ChatColor.YELLOW + "Jail deletion canceled.");
        }

        delJailCommand.getPendingDeletions().remove(playerUUID);
        return true;
    }
}