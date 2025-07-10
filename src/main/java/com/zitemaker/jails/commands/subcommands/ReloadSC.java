package com.zitemaker.jails.commands.subcommands;

import com.zitemaker.jails.JailsFree;
import com.zitemaker.jails.interfaces.SubCommandExecutor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadSC implements SubCommandExecutor {
    private final JailsFree plugin;

    public ReloadSC(JailsFree plugin) {
        this.plugin = plugin;
    }

    public void onSubCommand(@NotNull CommandSender sender, String[] args) {
        if (sender.hasPermission("jails.reload")) {
            try {
                plugin.reloadPluginConfig();
                sender.sendMessage(plugin.getTranslationManager().getMessage("config_reload_success"));
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to reload config.yml: " + e.getMessage());
                sender.sendMessage(ChatColor.RED + "An error occurred while reloading the configuration.");
            }

            try {
                plugin.getTranslationManager().restoreLanguageFiles();
                plugin.getTranslationManager().reloadMessages();
                sender.sendMessage(plugin.getTranslationManager().getMessage("messages_reload_success"));
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to reload language files: " + e.getMessage());
                sender.sendMessage(ChatColor.RED + "An error occurred while reloading language messages.");
            }

        } else {
            sender.sendMessage(plugin.getTranslationManager().getMessage("no_permission"));
        }

    }
}