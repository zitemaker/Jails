package me.zitemaker.jail.confirmations;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.commands.DelJailCommand;
import me.zitemaker.jail.listeners.TranslationManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HandleDelJailCommand implements CommandExecutor {
    private final JailPlugin plugin;
    private final DelJailCommand delJailCommand;
    private final TranslationManager translationManager;
    private final String prefix;

    public HandleDelJailCommand(JailPlugin plugin, DelJailCommand delJailCommand) {
        this.plugin = plugin;
        this.delJailCommand = delJailCommand;
        this.translationManager = plugin.getTranslationManager();
        this.prefix = plugin.getPrefix();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("handledeljail_only_players"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1 || (!args[0].equalsIgnoreCase("yes") && !args[0].equalsIgnoreCase("no"))) {
            player.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("handledeljail_usage"));
            return true;
        }

        String action = args[0].toLowerCase();
        UUID playerUUID = player.getUniqueId();
        String jailName = delJailCommand.getPendingDeletions().get(playerUUID);

        if (jailName == null) {
            player.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("handledeljail_no_pending"));
            return true;
        }

        if (action.equals("yes")) {
            if (!plugin.getJails().containsKey(jailName)) {
                String msg = String.format(translationManager.getMessage("handledeljail_jail_not_found"), jailName);
                player.sendMessage(prefix + " " + ChatColor.RED + msg);
            } else {
                plugin.removeJail(jailName);
                String msg = String.format(translationManager.getMessage("handledeljail_success"), jailName);
                player.sendMessage(prefix + " " + ChatColor.GREEN + msg);
            }
        } else {
            player.sendMessage(prefix + " " + ChatColor.YELLOW + translationManager.getMessage("handledeljail_canceled"));
        }

        delJailCommand.getPendingDeletions().remove(playerUUID);
        return true;
    }
}