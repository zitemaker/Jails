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
import java.util.ArrayList;
import java.util.List;

public class FlagList implements CommandExecutor, Listener {

    private final JailPlugin plugin;
    private final File flagsFile;
    private final FileConfiguration flagsConfig;

    public FlagList(JailPlugin plugin) {
        this.plugin = plugin;

        this.flagsFile = new File(plugin.getDataFolder(), "flags.yml");
        if (!flagsFile.exists()) {
            try {
                flagsFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().severe("Could not create flags.yml!");
                e.printStackTrace();
            }
        }
        this.flagsConfig = YamlConfiguration.loadConfiguration(flagsFile);

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
        openFlagListGUI(player);
        return true;
    }

    private void openFlagListGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.YELLOW + "Flags List");

        for (String flagName : flagsConfig.getKeys(false)) {
            String worldName = flagsConfig.getString(flagName + ".world");
            String pos1 = flagsConfig.getString(flagName + ".pos1");
            String pos2 = flagsConfig.getString(flagName + ".pos2");

            if (worldName == null || pos1 == null || pos2 == null) continue;

            plugin.getLogger().info("Fetching world: " + worldName);

            Material material;
            switch (worldName.toLowerCase()) {
                case "world":
                    material = Material.GRASS_BLOCK;
                    break;
                case "world_nether":
                    material = Material.NETHERRACK;
                    break;
                case "world_the_end":
                    material = Material.END_STONE;
                    break;
                default:
                    material = Material.BARRIER;
                    break;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + flagName);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.YELLOW + "Coords:");
                lore.add(ChatColor.YELLOW + pos1);
                lore.add(ChatColor.YELLOW + pos2);
                lore.add("");
                lore.add(ChatColor.GREEN + "Left-click to teleport");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            gui.addItem(item);
        }

        player.openInventory(gui);
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

        if (worldName == null || pos1 == null) return;

        String[] coords = pos1.split(",");
        if (coords.length != 3) return;

        try {
            World world = Bukkit.getWorld(worldName);
            double x = Double.parseDouble(coords[0]);
            double y = Double.parseDouble(coords[1]);
            double z = Double.parseDouble(coords[2]);

            if (event.isLeftClick() && world != null) {
                player.teleport(world.getBlockAt((int) x, (int) y, (int) z).getLocation());
                player.sendMessage(ChatColor.GREEN + "Teleported to flag: " + flagName);
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid coordinates for flag: " + flagName);
        }
    }
}