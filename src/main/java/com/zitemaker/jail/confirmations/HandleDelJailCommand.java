package com.zitemaker.jail.confirmations;

import com.zitemaker.jail.JailPlugin;
import com.zitemaker.jail.commands.DelJailCommand;
import com.zitemaker.jail.translation.TranslationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(translationManager.getMessage("handledeljail_only_players").replace("{prefix}", prefix));
            return true;
        }

        if (args.length != 1 || (!args[0].equalsIgnoreCase("yes") && !args[0].equalsIgnoreCase("no"))) {
            player.sendMessage(translationManager.getMessage("handledeljail_usage").replace("{prefix}", prefix));
            return true;
        }

        String action = args[0].toLowerCase();
        UUID playerUUID = player.getUniqueId();
        String jailName = delJailCommand.getPendingDeletions().get(playerUUID);

        if (jailName == null) {
            player.sendMessage(translationManager.getMessage("handledeljail_no_pending").replace("{prefix}", prefix));
            return true;
        }

        if (action.equals("yes")) {
            if (!plugin.getJails().containsKey(jailName)) {
                String msg = String.format(translationManager.getMessage("handledeljail_jail_not_found").replace("{prefix}", prefix), jailName);
                player.sendMessage(msg);
            } else {
                plugin.removeJail(jailName);
                String msg = String.format(translationManager.getMessage("handledeljail_success").replace("{prefix}", prefix), jailName);
                player.sendMessage(msg);
            }
        } else {
            player.sendMessage(translationManager.getMessage("handledeljail_canceled").replace("{prefix}", prefix));
        }

        delJailCommand.getPendingDeletions().remove(playerUUID);
        return true;
    }
}