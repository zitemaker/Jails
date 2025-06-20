package com.zitemaker.jail.commands;

import com.zitemaker.jail.JailPlugin;
import com.zitemaker.jail.translation.TranslationManager;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
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

        Objects.requireNonNull(plugin.getCommand("handcuff")).setExecutor(this);
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(translationManager.getMessage("handcuff_only_players").replace("{prefix}", prefix));
            return true;
        }

        if (!player.hasPermission("jails.handcuff")) {
            player.sendMessage(translationManager.getMessage("handcuff_no_permission").replace("{prefix}", prefix));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(translationManager.getMessage("handcuff_usage").replace("{prefix}", prefix));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(translationManager.getMessage("handcuff_player_not_found").replace("{prefix}", prefix));
            return true;
        }

        UUID targetUUID = target.getUniqueId();

        if (plugin.isPlayerHandcuffed(targetUUID)) {
            String msg = String.format(translationManager.getMessage("handcuff_already_handcuffed").replace("{prefix}", prefix), target.getName());
            player.sendMessage(msg);
            return true;
        }

        plugin.handcuffPlayer(target);

        String successMsg = String.format(translationManager.getMessage("handcuff_success").replace("{prefix}", prefix), target.getName());
        player.sendMessage(successMsg);

        String notificationMsg = String.format(translationManager.getMessage("handcuff_notification").replace("{prefix}", prefix), player.getName());
        target.sendMessage(notificationMsg);

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
            player.sendMessage(translationManager.getMessage("handcuff_no_break").replace("{prefix}", plugin.getPrefix()));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (disableBlockPlace && plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(translationManager.getMessage("handcuff_no_place").replace("{prefix}", plugin.getPrefix()));
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (disablePvp && event.getDamager() instanceof Player player
                && plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(translationManager.getMessage("handcuff_no_pvp").replace("{prefix}", plugin.getPrefix()));
        }
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (disableItemUse && item != null && !item.getType().isBlock()
                && plugin.isPlayerHandcuffed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(translationManager.getMessage("handcuff_no_item_use").replace("{prefix}", plugin.getPrefix()));
        }
    }
}