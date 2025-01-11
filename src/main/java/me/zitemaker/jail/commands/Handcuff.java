package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class Handcuff implements CommandExecutor, Listener {

    private final JailPlugin plugin;
    private final Set<Player> handcuffedPlayers = new HashSet<>();

    public Handcuff(JailPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("handcuff").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player handcuffer = (Player) sender;

        if (args.length < 1) {
            handcuffer.sendMessage(ChatColor.RED + "Usage: /handcuff <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            handcuffer.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        if (target.equals(handcuffer)) {
            handcuffer.sendMessage(ChatColor.RED + "You cannot handcuff yourself!");
            return true;
        }

        if (handcuffedPlayers.add(target)) {

            target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.01);

            handcuffer.sendMessage(ChatColor.GREEN + target.getName() + " has been handcuffed!");
            target.sendMessage(ChatColor.RED + "You have been handcuffed by " + handcuffer.getName() + "!");
        } else {

            target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.2);
            handcuffedPlayers.remove(target);

            handcuffer.sendMessage(ChatColor.YELLOW + target.getName() + " has been freed from handcuffs!");
            target.sendMessage(ChatColor.YELLOW + "You have been freed by " + handcuffer.getName() + "!");
        }

        return true;
    }


}
