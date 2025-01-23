package me.zitemaker.jail.flags;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FlagList implements CommandExecutor, Listener {

    private final JailPlugin plugin;
    private FileConfiguration flagsConfig;

    public FlagList(JailPlugin plugin) {
        this.plugin = plugin;
        this.flagsConfig = plugin.getFlagsConfig();

        plugin.initializeFlagsFile();

        plugin.getCommand("jailflaglist").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;


        plugin.reloadFlagsConfig();
        this.flagsConfig = plugin.getFlagsConfig();

        openFlagListGUI(player);
        return true;
    }

    private void openFlagListGUI(Player player) {

        this.flagsConfig = plugin.getFlagsConfig();

        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.YELLOW + "Flags List");
        Set<String> flagKeys = flagsConfig.getKeys(false);
        plugin.getLogger().info("Opening GUI with " + flagKeys.size() + " flags");

        for (String flagName : flagKeys) {
            String worldName = flagsConfig.getString(flagName + ".world");
            String pos1 = flagsConfig.getString(flagName + ".pos1");
            String pos2 = flagsConfig.getString(flagName + ".pos2");

            if (worldName == null || pos1 == null || pos2 == null) {
                plugin.getLogger().warning("Invalid flag data for " + flagName);
                plugin.getLogger().warning("World: " + worldName + ", Pos1: " + pos1 + ", Pos2: " + pos2);
                continue;
            }

            Material material = getMaterialForWorld(worldName);
            ItemStack item = createFlagItem(flagName, worldName, pos1, pos2, material);

            if (item != null) {
                gui.addItem(item);
                plugin.getLogger().info("Added flag item to GUI: " + flagName);
            }
        }

        player.openInventory(gui);
    }


    private Material getMaterialForWorld(String worldName) {
        switch (worldName.toLowerCase()) {
            case "world":
                return Material.GRASS_BLOCK;
            case "world_nether":
                return Material.NETHERRACK;
            case "world_the_end":
                return Material.END_STONE;
            default:
                return Material.BARRIER;
        }
    }

    private ItemStack createFlagItem(String flagName, String worldName, String pos1, String pos2, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            plugin.getLogger().warning("Failed to create ItemMeta for flag: " + flagName);
            return null;
        }

        meta.setDisplayName(ChatColor.YELLOW + flagName);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "World: " + ChatColor.WHITE + worldName);
        lore.add(ChatColor.YELLOW + "Coords:");
        lore.add(ChatColor.GOLD + "Pos1: " + pos1);
        lore.add(ChatColor.GOLD + "Pos2: " + pos2);
        lore.add("");
        lore.add(ChatColor.GREEN + "Left-click to teleport");
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ChatColor.stripColor(event.getView().getTitle()).equals("Flags List")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemMeta meta = clickedItem.getItemMeta();

        if (meta == null || !meta.hasDisplayName()) return;

        String flagName = ChatColor.stripColor(meta.getDisplayName());
        String worldName = flagsConfig.getString(flagName + ".world");
        String pos1 = flagsConfig.getString(flagName + ".pos1");

        if (worldName == null || pos1 == null) {
            player.sendMessage(ChatColor.RED + "Could not find flag data for: " + flagName);
            return;
        }

        handleFlagTeleport(player, flagName, worldName, pos1);
    }

    private void handleFlagTeleport(Player player, String flagName, String worldName, String pos1) {
        String[] coords = pos1.split(",");
        if (coords.length != 3) {
            player.sendMessage(ChatColor.RED + "Invalid coordinate format for flag: " + flagName);
            return;
        }

        try {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage(ChatColor.RED + "World not found: " + worldName);
                return;
            }

            double x = Double.parseDouble(coords[0]);
            double y = Double.parseDouble(coords[1]);
            double z = Double.parseDouble(coords[2]);

            player.teleport(world.getBlockAt((int) x, (int) y, (int) z).getLocation());
            player.sendMessage(ChatColor.GREEN + "Teleported to flag: " + flagName);

        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid coordinates for flag: " + flagName);
            plugin.getLogger().warning("Invalid coordinates for flag " + flagName + ": " + pos1);
        }
    }
}