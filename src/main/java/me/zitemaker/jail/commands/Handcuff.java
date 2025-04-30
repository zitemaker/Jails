package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Handcuff implements CommandExecutor, Listener {

    private final JailPlugin plugin;

    private boolean disableBlockBreak;
    private boolean disableBlockPlace;
    private boolean disablePvp;
    private boolean disableItemUse;

    public Handcuff(JailPlugin plugin) {
        this.plugin = plugin;

        plugin.getCommand("handcuff").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        reloadSettings();
    }

    public void reloadSettings() {
        var config = plugin.getConfig();
        this.disableBlockBreak = config.getBoolean("handcuff-settings.disable-block-break", true);
        this.disableBlockPlace = config.getBoolean("handcuff-settings.disable-block-place", true);
        this.disablePvp = config.getBoolean("handcuff-settings.disable-pvp", true);
        this.disableItemUse = config.getBoolean("handcuff-settings.disable-items", true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("jails.handcuff")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /handcuff <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        UUID targetUUID = target.getUniqueId();

        if (plugin.isPlayerHandcuffed(targetUUID)) {
            player.sendMessage(ChatColor.YELLOW + target.getName() + " is already handcuffed!");
            return true;
        }

        plugin.handcuffPlayer(target);

        player.sendMessage(ChatColor.GREEN + target.getName() + " has been handcuffed!");
        target.sendMessage(ChatColor.RED + "You have been handcuffed by " + player.getName() + "!");

        String broadcastTemplate = plugin.getConfig().getString(
                "handcuff-settings.broadcast-message",
                "{prefix} &c{player} has been handcuffed by {handcuffer}!"
        );

        String broadcastMessage = broadcastTemplate
                .replace("{prefix}", plugin.getPrefix())
                .replace("{player}", target.getName())
                .replace("{handcuffer}", player.getName());

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
        return true;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (disableBlockBreak && plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot break blocks while handcuffed!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (disableBlockPlace && plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot place blocks while handcuffed!");
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (disablePvp && event.getDamager() instanceof Player player
                && plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot attack others while handcuffed!");
        }
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (disableItemUse && item != null && !item.getType().isBlock()
                && plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot use items while handcuffed!");
        }
    }
}
