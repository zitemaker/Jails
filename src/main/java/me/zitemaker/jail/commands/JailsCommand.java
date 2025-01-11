package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class JailsCommand implements CommandExecutor {

    private final JailPlugin plugin;

    public JailsCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("jailplugin.jails")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (plugin.getJails().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No jail locations have been set.");
            return true;
        }

        Inventory jailGUI = Bukkit.createInventory(null, 54, ChatColor.RED + "Jails");

        plugin.getJails().forEach((name, location) -> {
            Material material;
            switch (location.getWorld().getEnvironment()) {
                case NETHER:
                    material = Material.NETHERRACK;
                    break;
                case THE_END:
                    material = Material.END_STONE;
                    break;
                default:
                    material = Material.GRASS_BLOCK;
                    break;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + "Name: " + ChatColor.GOLD + name);

                List<String> lore = List.of(
                        ChatColor.YELLOW + "Coords:",
                        ChatColor.GOLD + String.format("[%.1f, %.1f, %.1f]", location.getX(), location.getY(), location.getZ()),
                        "",
                        ChatColor.GREEN + "Left-click to teleport"
                );

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            jailGUI.addItem(item);
        });

        player.openInventory(jailGUI);
        return true;
    }

    public static void handleInventoryClick(Player player, ItemStack clickedItem, JailPlugin plugin) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) {
            return;
        }

        String displayName = ChatColor.stripColor(meta.getDisplayName());
        if (displayName.startsWith("Name: ")) {
            String jailName = displayName.substring(6).trim();
            Location location = plugin.getJails().get(jailName);

            if (location != null) {
                player.teleport(location);
                player.sendMessage(ChatColor.GREEN + "Teleported to jail: " + ChatColor.GOLD + jailName);
            }
        }
    }
}