package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Handcuff implements CommandExecutor, Listener {

    private final JailPlugin plugin;
    private final Set<Player> handcuffedPlayers = new HashSet<>();
    private final File handcuffedPlayersFile;
    private final FileConfiguration handcuffedPlayersConfig;

    public Handcuff(JailPlugin plugin) {
        this.plugin = plugin;

        // Initialize the handcuffed players file and configuration
        handcuffedPlayersFile = new File(plugin.getDataFolder(), "handcuffed_players.yml");
        if (!handcuffedPlayersFile.exists()) {
            try {
                handcuffedPlayersFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        handcuffedPlayersConfig = YamlConfiguration.loadConfiguration(handcuffedPlayersFile);

        plugin.getCommand("handcuff").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player handcuffer = (Player) sender;

        if (args.length < 1) {
            handcuffer.sendMessage(ChatColor.RED + "Usage: /handcuff <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            handcuffer.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        if (target.equals(handcuffer)) {
            handcuffer.sendMessage(ChatColor.RED + "You cannot handcuff yourself!");
            return true;
        }

        if (handcuffedPlayers.add(target)) {
            target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.01);


            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 3, true, false));


            handcuffedPlayersConfig.set("handcuffed." + target.getUniqueId(), true);
            saveHandcuffedPlayersConfig();

            handcuffer.sendMessage(ChatColor.GREEN + target.getName() + " has been handcuffed!");
            target.sendMessage(ChatColor.RED + "You have been handcuffed by " + handcuffer.getName() + "!");
        } else {


            target.removePotionEffect(PotionEffectType.SLOW);

            handcuffedPlayers.remove(target);


            handcuffedPlayersConfig.set("handcuffed." + target.getUniqueId(), null);
            saveHandcuffedPlayersConfig();

            handcuffer.sendMessage(ChatColor.YELLOW + target.getName() + " has been freed from handcuffs!");
            target.sendMessage(ChatColor.YELLOW + "You have been freed by " + handcuffer.getName() + "!");
        }

        return true;
    }

    private void saveHandcuffedPlayersConfig() {
        try {
            handcuffedPlayersConfig.save(handcuffedPlayersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (handcuffedPlayers.contains(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot break blocks while handcuffed!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (handcuffedPlayers.contains(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot place blocks while handcuffed!");
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (handcuffedPlayers.contains(player)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot attack others while handcuffed!");
            }
        }
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (handcuffedPlayers.contains(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot use items while handcuffed!");
        }
    }
}