package com.zitemaker.jail.commands;

import com.zitemaker.jail.JailPlugin;
import com.zitemaker.jail.translation.TranslationManager;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class JailsCommand implements CommandExecutor, Listener {

    private static final String GUI_TITLE = ChatColor.RED + "Jails";
    private static final int GUI_SIZE = 54;
    private static final String PERMISSION = "jails.jails";

    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public JailsCommand(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(translationManager.getMessage("jails_only_players").replace("{prefix}", prefix));
            return true;
        }

        if (!sender.hasPermission("jails.jails")) {
            player.sendMessage(translationManager.getMessage("jails_no_permission").replace("{prefix}", plugin.getPrefix()));
            return true;
        }

        Map<String, Location> jails = plugin.getJails();
        if (jails.isEmpty()) {
            player.sendMessage(translationManager.getMessage("jails_no_jails").replace("{prefix}", plugin.getPrefix()));
            return true;
        }

        Inventory jailGUI = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        for (Map.Entry<String, Location> entry : jails.entrySet()) {
            String name = entry.getKey();
            Location loc = entry.getValue();

            Material icon = switch (Objects.requireNonNull(loc.getWorld()).getEnvironment()) {
                case NETHER -> Material.NETHERRACK;
                case THE_END -> Material.END_STONE;
                default -> Material.GRASS_BLOCK;
            };

            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(translationManager.getMessage("jails_gui_name").replace("{prefix}", plugin.getPrefix()) + ChatColor.GOLD + name);
                meta.setLore(List.of(
                        translationManager.getMessage("jails_gui_coords").replace("{prefix}", plugin.getPrefix()),
                        ChatColor.GOLD + String.format("[%.1f, %.1f, %.1f]", loc.getX(), loc.getY(), loc.getZ()),
                        "",
                        translationManager.getMessage("jails_gui_left_click").replace("{prefix}", plugin.getPrefix())
                ));
                item.setItemMeta(meta);
                jailGUI.addItem(item);
            }
        }

        player.openInventory(jailGUI);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!ChatColor.stripColor(event.getView().getTitle()).equals("Jails")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        if (event.getClick() != ClickType.LEFT) {
            player.sendMessage(translationManager.getMessage("jails_left_click_only").replace("{prefix}", plugin.getPrefix()));
            return;
        }

        String strippedName = ChatColor.stripColor(meta.getDisplayName());
        if (!strippedName.startsWith(translationManager.getMessage("jails_gui_name"))) return;

        String jailName = strippedName.substring(translationManager.getMessage("jails_gui_name").length()).trim();
        Location jailLocation = plugin.getJails().get(jailName);

        if (jailLocation != null) {
            player.teleport(jailLocation);
            String msg = String.format(translationManager.getMessage("jails_teleported").replace("{prefix}", plugin.getPrefix()), jailName);
            player.sendMessage(ChatColor.GREEN + msg);
        } else {
            player.sendMessage(translationManager.getMessage("jails_location_not_found").replace("{prefix}", plugin.getPrefix()));
        }
    }
}