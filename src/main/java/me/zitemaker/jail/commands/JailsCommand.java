package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JailsCommand implements CommandExecutor {

    private final JailPlugin plugin;

    public JailsCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jailplugin.jails")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (plugin.getJails().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No jail locations have been set.");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Available Jails:");
            plugin.getJails().forEach((name, location) -> {
                String tpCommand = String.format("/tp %.1f %.1f %.1f", location.getX(), location.getY(), location.getZ());
                TextComponent message = new TextComponent(ChatColor.GOLD + "- " + name + ": " +
                        ChatColor.YELLOW + location.getWorld().getName() + " " +
                        ChatColor.AQUA + String.format("[%.1f, %.1f, %.1f]", location.getX(), location.getY(), location.getZ()));
                message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tpCommand));
                if (sender instanceof Player) {
                    ((Player) sender).spigot().sendMessage(message);
                } else {
                    sender.sendMessage(ChatColor.GOLD + "- " + name + ": " +
                            ChatColor.YELLOW + location.getWorld().getName() + " " +
                            ChatColor.AQUA + String.format("[%.1f, %.1f, %.1f]", location.getX(), location.getY(), location.getZ()));
                }
            });
        }
        return true;
    }
}