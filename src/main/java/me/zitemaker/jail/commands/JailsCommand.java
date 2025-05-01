package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
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

    public JailsCommand(JailPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        Map<String, Location> jails = plugin.getJails();
        if (jails.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No jail locations have been set.");
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
                meta.setDisplayName(ChatColor.YELLOW + "Name: " + ChatColor.GOLD + name);
                meta.setLore(List.of(
                        ChatColor.YELLOW + "Coords:",
                        ChatColor.GOLD + String.format("[%.1f, %.1f, %.1f]", loc.getX(), loc.getY(), loc.getZ()),
                        "",
                        ChatColor.GREEN + "Left-click to teleport"
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
            player.sendMessage(ChatColor.RED + "Please left-click to teleport.");
            return;
        }

        String strippedName = ChatColor.stripColor(meta.getDisplayName());
        if (!strippedName.startsWith("Name: ")) return;

        String jailName = strippedName.substring(6).trim();
        Location jailLocation = plugin.getJails().get(jailName);

        if (jailLocation != null) {
            player.teleport(jailLocation);
            player.sendMessage(ChatColor.GREEN + "Teleported to jail: " + ChatColor.GOLD + jailName);
        } else {
            player.sendMessage(ChatColor.RED + "Jail location not found!");
        }
    }
}
