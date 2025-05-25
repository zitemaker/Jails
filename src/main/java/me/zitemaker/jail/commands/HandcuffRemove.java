package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.Attribute;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class HandcuffRemove implements CommandExecutor {

    private final JailPlugin plugin;


    public HandcuffRemove(JailPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("unhandcuff").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if(!sender.hasPermission("jails.unhandcuff")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return false;
        }

        Player remover = (Player) sender;

        if (args.length < 1) {
            remover.sendMessage(ChatColor.RED + "Usage: /unhandcuff <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            remover.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        UUID targetUUID = target.getUniqueId();


        if (!plugin.isPlayerHandcuffed(targetUUID)) {
            remover.sendMessage(ChatColor.RED + target.getName() + " is not handcuffed!");
            return true;
        }


        target.removePotionEffect(PotionEffectType.SLOW);
        target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);

        plugin.unHandcuffPlayer(targetUUID);


        remover.sendMessage(ChatColor.GREEN + target.getName() + " has been removed from the handcuffed list.");
        target.sendMessage(ChatColor.GREEN + "You have been removed from the handcuffed list.");

        return true;
    }


}
