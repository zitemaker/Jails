package com.zitemaker.jails.commands.subcommands;

import com.zitemaker.jails.JailsFree;
import com.zitemaker.jails.interfaces.SubCommandExecutor;
import com.zitemaker.jails.translation.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class UnHandcuffSC implements SubCommandExecutor {

    private final JailsFree plugin;
    private final TranslationManager translationManager;

    public UnHandcuffSC(JailsFree plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @Override
    public void onSubCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        String prefix = plugin.getPrefix();

        if (!(sender instanceof Player remover)) {
            sender.sendMessage(translationManager.getMessage("unhandcuff_only_players").replace("{prefix}", prefix));
            return;
        }

        if (!remover.hasPermission("jails.unhandcuff")) {
            remover.sendMessage(translationManager.getMessage("unhandcuff_no_permission").replace("{prefix}", prefix));
            return;
        }

        if (args.length < 1) {
            remover.sendMessage(translationManager.getMessage("unhandcuff_usage").replace("{prefix}", prefix));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            remover.sendMessage(translationManager.getMessage("unhandcuff_player_not_found").replace("{prefix}", prefix));
            return;
        }

        UUID targetUUID = target.getUniqueId();

        if (!plugin.isPlayerHandcuffed(targetUUID)) {
            String msg = String.format(translationManager.getMessage("unhandcuff_not_handcuffed").replace("{prefix}", prefix), target.getName());
            remover.sendMessage(msg);
            return;
        }

        target.removePotionEffect(PotionEffectType.SLOW);
        Objects.requireNonNull(target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.1);

        plugin.unHandcuffPlayer(targetUUID);

        String successMsg = String.format(translationManager.getMessage("unhandcuff_success").replace("{prefix}", prefix), target.getName());
        remover.sendMessage(successMsg);

        target.sendMessage(translationManager.getMessage("unhandcuff_notification").replace("{prefix}", prefix));
    }
}