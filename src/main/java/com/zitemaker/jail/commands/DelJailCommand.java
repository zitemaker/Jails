package com.zitemaker.jail.commands;

import com.zitemaker.jail.JailPlugin;
import com.zitemaker.jail.translation.TranslationManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class DelJailCommand implements CommandExecutor {
    private final JailPlugin plugin;
    private final TranslationManager translationManager;
    private final String prefix;
    private final Map<UUID, String> pendingDeletions = new ConcurrentHashMap<>();

    public DelJailCommand(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
        this.prefix = plugin.getPrefix();
    }

    public Map<UUID, String> getPendingDeletions() {
        return pendingDeletions;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("jails.deljail")) {
            sender.sendMessage(translationManager.getMessage("deljail_no_permission").replace("{prefix}", prefix));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(translationManager.getMessage("deljail_usage").replace("{prefix}", prefix));
            return true;
        }

        String jailName = args[0].toLowerCase();

        if (!plugin.getJails().containsKey(jailName)) {
            String msg = String.format(translationManager.getMessage("deljail_jail_not_found").replace("{prefix}", prefix), jailName);
            sender.sendMessage(msg);
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.removeJail(jailName);
            String msg = String.format(translationManager.getMessage("deljail_success").replace("{prefix}", prefix), jailName);
            sender.sendMessage(msg);
            return true;
        }

        pendingDeletions.put(player.getUniqueId(), jailName);

        String prompt = String.format(translationManager.getMessage("deljail_confirmation_prompt").replace("{prefix}", prefix), jailName);
        TextComponent message = new TextComponent(prompt);
        message.setColor(ChatColor.GOLD);

        TextComponent yes = new TextComponent(" [CONFIRM] ");
        yes.setColor(ChatColor.GREEN);
        yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/handledeljail yes"));
        yes.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(translationManager.getMessage("deljail_confirm_hover").replace("{prefix}", prefix))
                        .color(ChatColor.GREEN).create()));

        TextComponent no = new TextComponent("[CANCEL] ");
        no.setColor(ChatColor.RED);
        no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/handledeljail no"));
        no.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(translationManager.getMessage("deljail_cancel_hover").replace("{prefix}", prefix))
                        .color(ChatColor.RED).create()));

        TextComponent spacing = new TextComponent("     ");

        player.spigot().sendMessage(new ComponentBuilder("")
                .append(message).append("\n")
                .append(spacing).append(yes).append("  ").append(no)
                .create());

        return true;
    }
}