package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class Handcuff implements CommandExecutor, Listener {

    private final JailPlugin plugin;

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

        UUID targetUUID = target.getUniqueId();
        if (!plugin.isPlayerHandcuffed(targetUUID)) {
            plugin.handcuffPlayer(target);
            handcuffer.sendMessage(ChatColor.GREEN + target.getName() + " has been handcuffed!");
            target.sendMessage(ChatColor.RED + "You have been handcuffed by " + handcuffer.getName() + "!");
        } else {
            handcuffer.sendMessage(ChatColor.YELLOW + target.getName() + " is already handcuffed!");
        }

        return true;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot break blocks while handcuffed!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot place blocks while handcuffed!");
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (plugin.isPlayerHandcuffed(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot attack others while handcuffed!");
            }
        }
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot use items while handcuffed!");
        }
    }
}
