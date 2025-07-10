package com.zitemaker.jails.commands.subcommands;

import com.zitemaker.jails.JailsFree;
import com.zitemaker.jails.interfaces.SubCommandExecutor;
import com.zitemaker.jails.translation.TranslationManager;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class DeleteJailSC implements SubCommandExecutor {
    private final JailsFree plugin;
    private final TranslationManager translationManager;
    private final String prefix;
    @Getter
    private final Map<UUID, String> pendingDeletions = new ConcurrentHashMap<>();

    public DeleteJailSC(JailsFree plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
        this.prefix = plugin.getPrefix();
    }

    @Override
    public void onSubCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jails.deljail")) {
            sender.sendMessage(translationManager.getMessage("deljail_no_permission").replace("{prefix}", prefix));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(translationManager.getMessage("deljail_usage").replace("{prefix}", prefix));
            return;
        }

        String jailName = args[0].toLowerCase();

        if (!plugin.getJails().containsKey(jailName)) {
            String msg = String.format(translationManager.getMessage("deljail_jail_not_found").replace("{prefix}", prefix), jailName);
            sender.sendMessage(msg);
            return;
        }

        if (!(sender instanceof Player player)) {
            plugin.removeJail(jailName);
            String msg = String.format(translationManager.getMessage("deljail_success").replace("{prefix}", prefix), jailName);
            sender.sendMessage(msg);
            return;
        }

        pendingDeletions.put(player.getUniqueId(), jailName);

        String prompt = String.format(translationManager.getMessage("deljail_confirmation_prompt").replace("{prefix}", prefix), jailName);
        TextComponent message = new TextComponent(prompt);
        message.setColor(ChatColor.GOLD);

        TextComponent yes = new TextComponent(" [CONFIRM] ");
        yes.setColor(ChatColor.GREEN);
        yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/deljailcf yes"));
        yes.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(translationManager.getMessage("deljail_confirm_hover").replace("{prefix}", prefix))
                        .color(ChatColor.GREEN).create()));

        TextComponent no = new TextComponent("[CANCEL] ");
        no.setColor(ChatColor.RED);
        no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/deljailcf no"));
        no.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(translationManager.getMessage("deljail_cancel_hover").replace("{prefix}", prefix))
                        .color(ChatColor.RED).create()));

        TextComponent spacing = new TextComponent("     ");

        player.spigot().sendMessage(new ComponentBuilder("")
                .append(message).append("\n")
                .append(spacing).append(yes).append("  ").append(no)
                .create());

    }
}