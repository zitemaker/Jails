package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
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
    private final TranslationManager translationManager;

    private boolean disableBlockBreak;
    private boolean disableBlockPlace;
    private boolean disablePvp;
    private boolean disableItemUse;

    public Handcuff(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();

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
        String prefix = plugin.getPrefix();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("handcuff_only_players"));
            return true;
        }

        if (!player.hasPermission("jails.handcuff")) {
            player.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("handcuff_no_permission"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("handcuff_usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("handcuff_player_not_found"));
            return true;
        }

        UUID targetUUID = target.getUniqueId();

        if (plugin.isPlayerHandcuffed(targetUUID)) {
            String msg = String.format(translationManager.getMessage("handcuff_already_handcuffed"), target.getName());
            player.sendMessage(prefix + " " + ChatColor.YELLOW + msg);
            return true;
        }

        plugin.handcuffPlayer(target);

        String successMsg = String.format(translationManager.getMessage("handcuff_success"), target.getName());
        player.sendMessage(prefix + " " + ChatColor.GREEN + successMsg);

        String notificationMsg = String.format(translationManager.getMessage("handcuff_notification"), player.getName());
        target.sendMessage(prefix + " " + ChatColor.RED + notificationMsg);

        String broadcastTemplate = translationManager.getMessage("handcuff_broadcast");
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
            player.sendMessage(plugin.getPrefix() + " " + ChatColor.RED + translationManager.getMessage("handcuff_no_break"));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (disableBlockPlace && plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + " " + ChatColor.RED + translationManager.getMessage("handcuff_no_place"));
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (disablePvp && event.getDamager() instanceof Player player
                && plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + " " + ChatColor.RED + translationManager.getMessage("handcuff_no_pvp"));
        }
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (disableItemUse && item != null && !item.getType().isBlock()
                && plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + " " + ChatColor.RED + translationManager.getMessage("handcuff_no_item_use"));
        }
    }
}