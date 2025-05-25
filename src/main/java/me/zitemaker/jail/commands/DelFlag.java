package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelFlag implements CommandExecutor {
    private JailPlugin plugin;

    public DelFlag(JailPlugin plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(!(sender instanceof Player)){
            sender.sendMessage("You must be a player to use this command!");
            return false;
        }

        if (!sender.hasPermission("jails.delflag")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
        }

        Player player = (Player) sender;
        plugin.sendJailsPlusMessage(player);
        return true;
    }
}

