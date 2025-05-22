package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelFlag implements CommandExecutor {
    private JailPlugin plugin;
    private final TranslationManager translationManager;

    public DelFlag(JailPlugin plugin){
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(!(sender instanceof Player)){
            sender.sendMessage (translationManager.getMessage("df_player_check"));
            return false;
        }

        if (!sender.hasPermission("jails.delflag")) {
            sender.sendMessage(ChatColor.RED + translationManager.getMessage("df_no_permission"));
        }

        Player player = (Player) sender;
        plugin.sendJailsPlusMessage(player);
        return true;
    }
}