package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class DelJailCommand implements CommandExecutor {
    private final JailPlugin plugin;
    private final Map<UUID, String> pendingDeletions = new ConcurrentHashMap<>();

    public DelJailCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, String> getPendingDeletions() {
        return pendingDeletions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jails.deljail")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to delete jails.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: " + ChatColor.YELLOW + "/deljail <jail name>");
            return true;
        }

        String jailName = args[0].toLowerCase();

        if (!plugin.getJails().containsKey(jailName)) {
            sender.sendMessage(ChatColor.RED + "Jail '" + ChatColor.YELLOW + jailName + ChatColor.RED + "' does not exist.");
            return true;
        }

        if (!(sender instanceof Player)) {
            plugin.removeJail(jailName);
            sender.sendMessage(ChatColor.GREEN + "Jail " + ChatColor.YELLOW + jailName + ChatColor.GREEN + " has been deleted.");
            return true;
        }

        Player player = (Player) sender;
        pendingDeletions.put(player.getUniqueId(), jailName);

        TextComponent message = new TextComponent("Are you sure you want to delete the jail '" + jailName + "'?");
        message.setColor(ChatColor.GOLD);

        TextComponent yes = new TextComponent(" [CONFIRM] ");
        yes.setColor(ChatColor.GREEN);
        yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/handledeljail yes"));
        yes.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Click to confirm jail deletion").color(ChatColor.GREEN).create()));

        TextComponent no = new TextComponent("[CANCEL] ");
        no.setColor(ChatColor.RED);
        no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/handledeljail no"));
        no.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Click to cancel jail deletion").color(ChatColor.RED).create()));

        TextComponent spacing = new TextComponent("     ");

        player.spigot().sendMessage(new ComponentBuilder("")
                .append(message).append("\n")
                .append(spacing).append(yes).append("  ").append(no)
                .create());

        return true;
    }
}