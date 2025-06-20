package me.zitemaker.jail.commands;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class HandcuffRemove implements CommandExecutor {

    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public HandcuffRemove(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
        Objects.requireNonNull(plugin.getCommand("unhandcuff")).setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String prefix = plugin.getPrefix();

        if (!(sender instanceof Player remover)) {
            sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("unhandcuff_only_players"));
            return true;
        }

        if (!remover.hasPermission("jails.unhandcuff")) {
            remover.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("unhandcuff_no_permission"));
            return true;
        }

        if (args.length < 1) {
            remover.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("unhandcuff_usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            remover.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("unhandcuff_player_not_found"));
            return true;
        }

        UUID targetUUID = target.getUniqueId();

        if (!plugin.isPlayerHandcuffed(targetUUID)) {
            String msg = String.format(translationManager.getMessage("unhandcuff_not_handcuffed"), target.getName());
            remover.sendMessage(prefix + " " + ChatColor.RED + msg);
            return true;
        }

        target.removePotionEffect(PotionEffectType.SLOW);
        Objects.requireNonNull(target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.1);

        plugin.unHandcuffPlayer(targetUUID);

        String successMsg = String.format(translationManager.getMessage("unhandcuff_success"), target.getName());
        remover.sendMessage(prefix + " " + ChatColor.GREEN + successMsg);

        target.sendMessage(prefix + " " + ChatColor.GREEN + translationManager.getMessage("unhandcuff_notification"));

        return true;
    }
}