package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlagList implements CommandExecutor {
    private JailPlugin plugin;

    public FlagList(JailPlugin plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(!(sender instanceof Player)){
            sender.sendMessage("You must be a player to use this command!");
            return false;
        }
        if(!sender.hasPermission("jails.flaglist")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return false;
        }
        Player player = (Player) sender;
        plugin.sendJailsPlusMessage(player);
        return true;
    }
}

