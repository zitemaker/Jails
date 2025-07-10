package com.zitemaker.jails.commands.subcommands;

import com.zitemaker.jails.JailsFree;
import com.zitemaker.jails.translation.TranslationManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import com.zitemaker.jails.interfaces.SubCommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class UnjailSC implements SubCommandExecutor {

    private final JailsFree plugin;
    private final TranslationManager translationManager;
    private final String prefix;

    public UnjailSC(JailsFree plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
        this.prefix = plugin.getPrefix();
    }

    @Override
    public void onSubCommand(@NotNull CommandSender sender, String[] args) {
        if (!sender.hasPermission("jails.unjail")) {
            sender.sendMessage(translationManager.getMessage("unjail_no_permission").replace("{prefix}", prefix));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(translationManager.getMessage("unjail_usage").replace("{prefix}", prefix));
            return;
        }

        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(translationManager.getMessage("unjail_player_not_found").replace("{prefix}", prefix));
            return;
        }

        UUID targetUUID = target.getUniqueId();
        if (!plugin.isPlayerJailed(targetUUID)) {
            String msg = String.format(translationManager.getMessage("unjail_player_not_jailed").replace("{prefix}", prefix), target.getName());
            sender.sendMessage(msg);
            return;
        }

        if (sender instanceof Player player) {
            UUID senderUUID = player.getUniqueId();

            String token = plugin.getUnjailCF().generateToken(senderUUID, targetUUID);

            String prompt = String.format(translationManager.getMessage("unjail_confirmation_prompt"), target.getName());
            TextComponent message = new TextComponent(prompt);
            message.setColor(ChatColor.GOLD);

            TextComponent yes = new TextComponent(" [CONFIRM] ");
            yes.setColor(ChatColor.GREEN);
            yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/confirmunjail " + token));
            yes.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(translationManager.getMessage("unjail_confirm_hover")).color(ChatColor.GREEN).create()));

            TextComponent no = new TextComponent("[CANCEL]");
            no.setColor(ChatColor.RED);
            no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cancelunjail " + token));
            no.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(translationManager.getMessage("unjail_cancel_hover")).color(ChatColor.RED).create()));

            TextComponent spacing = new TextComponent("     ");

            player.spigot().sendMessage(new ComponentBuilder("")
                    .append(message).append("\n")
                    .append(spacing).append(yes).append("  ").append(no)
                    .create());
        } else {
            plugin.unjailPlayer(targetUUID);
            String msg = String.format(translationManager.getMessage("unjail_success"), target.getName());
            sender.sendMessage(prefix + " " + ChatColor.GREEN + msg);
        }

    }
}