package com.zitemaker.jails.commands.subcommands;

import com.zitemaker.jails.JailsFree;
import com.zitemaker.jails.interfaces.SubCommandExecutor;
import com.zitemaker.jails.translation.TranslationManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JailSetSC implements SubCommandExecutor {
    private final JailsFree plugin;
    private final TranslationManager translationManager;

    public JailSetSC(JailsFree plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public void onSubCommand(@NotNull CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(translationManager.getMessage("setjail_only_players").replace("{prefix}", plugin.getPrefix()));
            return;
        }

        if (!sender.hasPermission("jails.setjail")) {
            sender.sendMessage(translationManager.getMessage("setjail_no_permission").replace("{prefix}", plugin.getPrefix()));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(translationManager.getMessage("setjail_usage").replace("{prefix}", plugin.getPrefix()));
            return;
        }

        Location location = player.getLocation();
        String jailName = args[0].toLowerCase();

        plugin.addJail(jailName, location);
        String successMsg = String.format(translationManager.getMessage("setjail_success").replace("{prefix}", plugin.getPrefix()), jailName);
        player.sendMessage(ChatColor.GREEN + successMsg);
    }
}