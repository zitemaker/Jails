package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class JailCommand implements CommandExecutor, Listener {

    private final JailPlugin plugin;
    private final Map<UUID, Player> jailSelections = new HashMap<>();

    public JailCommand(JailPlugin plugin) {
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

        if (!player.hasPermission("jails.jail")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /jail <player>");
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return false;
        }

        if (plugin.isPlayerJailed(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + target.getName() + " is already jailed!");
            return false;
        }

        jailSelections.put(player.getUniqueId(), target);
        openJailSelectionGUI(player);
        return true;
    }

    private void openJailSelectionGUI(Player player) {
        Inventory jailMenu = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Select a Jail");

        plugin.getJails().forEach((jailName, location) -> {
            ItemStack jailItem;
            World.Environment env = location.getWorld().getEnvironment();

            if (env == World.Environment.NORMAL) {
                jailItem = new ItemStack(Material.GRASS_BLOCK);
            } else if (env == World.Environment.NETHER) {
                jailItem = new ItemStack(Material.NETHERRACK);
            } else {
                jailItem = new ItemStack(Material.END_STONE);
            }

            ItemMeta meta = jailItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + jailName);
                meta.setLore(java.util.Arrays.asList(
                        ChatColor.GRAY + "Left-Click: Jail Player",
                        ChatColor.GRAY + "Right-Click: Teleport"
                ));
                jailItem.setItemMeta(meta);
            }

            jailMenu.addItem(jailItem);
        });

        player.openInventory(jailMenu);
    }

    @EventHandler
    public void onJailGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (inventory == null || clickedItem == null || !event.getView().getTitle().equals(ChatColor.RED + "Select a Jail")) {
            return;
        }

        event.setCancelled(true);
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }

        String jailName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        Location jailLocation = plugin.getJails().get(jailName);

        if (jailLocation == null) {
            player.sendMessage(ChatColor.RED + "Jail not found.");
            return;
        }

        if (event.isLeftClick()) {
            Player target = jailSelections.get(player.getUniqueId());
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player selection expired.");
                return;
            }

            plugin.jailPlayer(target, jailName, -1, "No reason provided", player.getName());

            String broadcastMessage = plugin.getConfig().getString("general.jail-broadcast-message",
                            "{prefix} &c{player} has been jailed permanently by {jailer}. Reason: {reason}!")
                    .replace("{prefix}", ChatColor.translateAlternateColorCodes('&', plugin.getPrefix()))
                    .replace("{player}", target.getName())
                    .replace("{jailer}", player.getName())
                    .replace("{reason}", "No reason provided");

            if (plugin.getConfig().getBoolean("general.broadcast-on-jail")) {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
            }

            target.sendMessage(ChatColor.RED + "You have been jailed permanently by " + player.getName() + ".");
            player.sendMessage(ChatColor.GREEN + "You jailed " + target.getName() + " in " + jailName + ".");
            jailSelections.remove(player.getUniqueId());
            player.closeInventory();

        } else if (event.isRightClick()) {
            player.teleport(jailLocation);
            player.sendMessage(ChatColor.GREEN + "Teleported to jail: " + jailName);
        }
    }
}
