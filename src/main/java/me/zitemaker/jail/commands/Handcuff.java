package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Handcuff implements CommandExecutor {

    private final JailPlugin plugin;

    public Handcuff(JailPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("handcuff").setExecutor(this);
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

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 60, 1)); // Slowness II for 60 seconds
        target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 60, 2)); // Turtle Master for 60 seconds

        target.sendMessage(ChatColor.RED + "You have been handcuffed by " + handcuffer.getName() + "!");
        handcuffer.sendMessage(ChatColor.GREEN + "You have handcuffed " + target.getName() + "!");

        return true;
    }
}