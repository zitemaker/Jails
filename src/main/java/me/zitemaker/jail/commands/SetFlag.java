package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.zitemaker.jail.listeners.TranslationManager;

public class SetFlag implements CommandExecutor {
    private JailPlugin plugin;
    private final TranslationManager translationManager;

    public SetFlag(JailPlugin plugin){
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + translationManager.getMessage("set_flag_only_players"));
            return false;
        }

        if(!sender.hasPermission("jails.setflag")){
            sender.sendMessage(ChatColor.RED + translationManager.getMessage("set_flag_no_permission"));
            return false;
        }
        Player player = (Player) sender;
        plugin.sendJailsPlusMessage(player);
        return true;
    }
}

