package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class JailListCommand implements CommandExecutor, TabCompleter, Listener {

    private final JailPlugin plugin;

    public JailListCommand(JailPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("jail.list")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatColor.RED + "Usage: /jailed list");
            return true;
        }


        FileConfiguration jailedConfig = plugin.getJailedPlayersConfig();
        List<String> jailedKeys = new ArrayList<>(jailedConfig.getKeys(false));

        Inventory jailList = Bukkit.createInventory(null, 54, ChatColor.RED + "Jail List");


        for (String key : jailedKeys) {
            UUID playerUUID = UUID.fromString(key);
            String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
            String jailName = jailedConfig.getString(key + ".jailName", "Unknown");
            String reason = jailedConfig.getString(key + ".reason", "No reason provided");
            long endTime = jailedConfig.getLong(key + ".endTime", -1);
            String duration = endTime == -1 ? "Permanent" : formatDuration(endTime - System.currentTimeMillis());


            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUUID));
                meta.setDisplayName(ChatColor.YELLOW + "Name: " + ChatColor.RED + playerName);

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.YELLOW + "Reason: " + ChatColor.RED + reason);
                lore.add(ChatColor.YELLOW + "Duration: " + ChatColor.RED + duration);
                lore.add(ChatColor.YELLOW + "Jail Name: " + ChatColor.RED + jailName);
                lore.add("");
                lore.add(ChatColor.GREEN + "Left Click to tp to the jail");
                lore.add(ChatColor.GREEN + "Right Click to set the player free");
                meta.setLore(lore);

                head.setItemMeta(meta);
            }

            jailList.addItem(head);
        }



        ItemStack filler = new ItemStack(Material.AIR);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.RED + "");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < jailList.getSize(); i++) {
            if (jailList.getItem(i) == null) {
                jailList.setItem(i, filler);
            }
        }

        player.openInventory(jailList);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ChatColor.stripColor(event.getView().getTitle()).equals("Jail List")) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();


        if (clickedItem == null || clickedItem.getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) return;


        if (event.isLeftClick()) {

            String jailName = ChatColor.stripColor(meta.getLore().get(2).split(":")[1].trim());
            Location jailLocation = plugin.getJail(jailName);
            if (jailLocation != null) {
                player.teleport(jailLocation);
                player.sendMessage(ChatColor.GREEN + "Teleported to the jail: " + jailName);
            } else {
                player.sendMessage(ChatColor.RED + "Jail location not found.");
            }
        } else if (event.isRightClick()) {

            UUID targetUUID = meta.getOwningPlayer().getUniqueId();
            plugin.unjailPlayer(targetUUID);
            player.sendMessage(ChatColor.GREEN + "You have set " + Bukkit.getOfflinePlayer(targetUUID).getName() + " free.");
        }
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("list");
        }
        return Collections.emptyList();
    }

    private String formatDuration(long durationMillis) {
        if (durationMillis <= 0) return "Expired";
        long seconds = durationMillis / 1000 % 60;
        long minutes = durationMillis / (1000 * 60) % 60;
        long hours = durationMillis / (1000 * 60 * 60) % 24;
        long days = durationMillis / (1000 * 60 * 60 * 24);

        return (days > 0 ? days + "d " : "") +
                (hours > 0 ? hours + "h " : "") +
                (minutes > 0 ? minutes + "m " : "") +
                (seconds > 0 ? seconds + "s" : "").trim();
    }
}