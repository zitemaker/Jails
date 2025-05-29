package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UnjailCommand implements CommandExecutor {

    private final JailPlugin plugin;
    private final TranslationManager translationManager;
    private final String prefix;

    public UnjailCommand(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
        this.prefix = plugin.getPrefix();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jails.unjail")) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("unjail_no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("unjail_usage"));
            return true;
        }

        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("unjail_player_not_found"));
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        if (!plugin.isPlayerJailed(targetUUID)) {
            String msg = String.format(translationManager.getMessage("unjail_player_not_jailed"), target.getName());
            sender.sendMessage(prefix + " " + ChatColor.YELLOW + msg);
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID senderUUID = player.getUniqueId();

            boolean isIpJailed = false;
            Player targetPlayer = target.getPlayer();
            if (targetPlayer != null && targetPlayer.isOnline()) {
                String hashedIp = plugin.getPlayerHashedIp(targetPlayer);
                isIpJailed = plugin.isIpJailed(hashedIp);
            }

            String token = plugin.unjailConfirmation.generateToken(senderUUID, targetUUID, isIpJailed);

            String prompt = String.format(translationManager.getMessage("unjail_confirmation_prompt"), target.getName());
            TextComponent message = new TextComponent(prompt);
            message.setColor(ChatColor.GOLD);

            if (isIpJailed) {
                String ipInfoText = translationManager.getMessage("unjail_ip_jailed_info");
                TextComponent ipInfo = new TextComponent(ipInfoText);
                ipInfo.setColor(ChatColor.LIGHT_PURPLE);
                message.addExtra("\n");
                message.addExtra(ipInfo);
            }

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

            Player targetPlayer = target.getPlayer();
            boolean wasIpJailed = false;
            if (targetPlayer != null && targetPlayer.isOnline()) {
                wasIpJailed = plugin.removePlayerIpJail(targetPlayer);
            }

            if (wasIpJailed) {
                String msg = String.format(translationManager.getMessage("unjail_success_ip_removed"), target.getName());
                sender.sendMessage(prefix + " " + ChatColor.GREEN + msg);
            } else {
                String msg = String.format(translationManager.getMessage("unjail_success"), target.getName());
                sender.sendMessage(prefix + " " + ChatColor.GREEN + msg);
            }
        }

        return true;
    }
}