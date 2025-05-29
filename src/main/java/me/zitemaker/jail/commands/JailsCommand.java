package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("jails_only_players"));
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("jails_no_permission"));
            return true;
        }

        Map<String, Location> jails = plugin.getJails();
        if (jails.isEmpty()) {
            player.sendMessage(prefix + " " + ChatColor.YELLOW + translationManager.getMessage("jails_no_jails"));
            return true;
        }

        Inventory jailGUI = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        for (Map.Entry<String, Location> entry : jails.entrySet()) {
            String name = entry.getKey();
            Location loc = entry.getValue();

            Material icon = switch (loc.getWorld().getEnvironment()) {
                case NETHER -> Material.NETHERRACK;
                case THE_END -> Material.END_STONE;
                default -> Material.GRASS_BLOCK;
            };

            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + translationManager.getMessage("jails_gui_name") + ChatColor.GOLD + name);
                meta.setLore(List.of(
                        ChatColor.YELLOW + translationManager.getMessage("jails_gui_coords"),
                        ChatColor.GOLD + String.format("[%.1f, %.1f, %.1f]", loc.getX(), loc.getY(), loc.getZ()),
                        "",
                        ChatColor.GREEN + translationManager.getMessage("jails_gui_left_click")
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
            player.sendMessage(ChatColor.RED + translationManager.getMessage("jails_left_click_only"));
            return;
        }

        String strippedName = ChatColor.stripColor(meta.getDisplayName());
        if (!strippedName.startsWith(translationManager.getMessage("jails_gui_name"))) return;

        String jailName = strippedName.substring(translationManager.getMessage("jails_gui_name").length()).trim();
        Location jailLocation = plugin.getJails().get(jailName);

        if (jailLocation != null) {
            player.teleport(jailLocation);
            String msg = String.format(translationManager.getMessage("jails_teleported"), jailName);
            player.sendMessage(ChatColor.GREEN + msg);
        } else {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("jails_location_not_found"));
        }
    }
}