package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
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

    public UnjailCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jails.unjail")) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "You do not have permission to unjail players.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Usage: " + org.bukkit.ChatColor.YELLOW + "/unjail <player>");
            return true;
        }

        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Player not found or has never joined the server.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        if (!plugin.isPlayerJailed(targetUUID)) {
            sender.sendMessage(org.bukkit.ChatColor.YELLOW + target.getName() + " is not currently jailed.");
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID senderUUID = player.getUniqueId();
            String token = plugin.unjailConfirmation.generateToken(senderUUID, targetUUID);

            TextComponent message = new TextComponent("Are you sure you want to unjail ");
            message.setColor(ChatColor.GOLD);

            TextComponent playerName = new TextComponent(target.getName());
            playerName.setColor(ChatColor.YELLOW);
            message.addExtra(playerName);
            message.addExtra("?");

            TextComponent yes = new TextComponent(" [CONFIRM] ");
            yes.setColor(ChatColor.GREEN);
            yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/confirmunjail " + token));
            yes.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Click to confirm unjailing").color(ChatColor.GREEN).create()));

            TextComponent no = new TextComponent("[CANCEL]");
            no.setColor(ChatColor.RED);
            no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cancelunjail " + token));
            no.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Click to cancel unjailing").color(ChatColor.RED).create()));

            player.spigot().sendMessage(new ComponentBuilder("")
                    .append(message).append("\n")
                    .append(yes).append(" ").append(no)
                    .create());
        } else {
            plugin.unjailPlayer(targetUUID);
            sender.sendMessage(org.bukkit.ChatColor.GREEN + "Player " + target.getName() + " has been unjailed.");
        }

        return true;
    }
}