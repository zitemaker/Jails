package com.zitemaker.jails.commands.confirmations;

import com.zitemaker.jails.JailsFree;
import com.zitemaker.jails.translation.TranslationManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class UnjailCF implements CommandExecutor {

    private final JailsFree plugin;
    private final TranslationManager translationManager;
    private final String prefix;
    private final Map<String, UnjailRequest> pendingConfirmations = new ConcurrentHashMap<>();

    public UnjailCF(JailsFree plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
        this.prefix = plugin.getPrefix();
    }

    public String generateToken(UUID senderUUID, UUID targetUUID) {
        String token = UUID.randomUUID().toString();
        pendingConfirmations.put(token, new UnjailRequest(senderUUID, targetUUID));
        return token;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(translationManager.getMessage("confirmation_only_players").replace("{prefix}", prefix));
            return true;
        }
        if (args.length != 1) {
            String usage = String.format(translationManager.getMessage("confirmation_usage"), label);
            sender.sendMessage(translationManager.getMessage("confirmation_usage").replace("{prefix}", prefix));
            return true;
        }

        String token = args[0];
        UnjailRequest request = pendingConfirmations.get(token);

        if (request == null) {
            sender.sendMessage(translationManager.getMessage("confirmation_invalid_token").replace("{prefix}", prefix));
            return true;
        }

        if (!player.getUniqueId().equals(request.senderUUID())) {
            sender.sendMessage(translationManager.getMessage("confirmation_not_authorized").replace("{prefix}", prefix));
            return true;
        }

        if (command.getName().equalsIgnoreCase("confirmunjail")) {
            UUID targetUUID = request.targetUUID();
            if (!plugin.isPlayerJailed(targetUUID)) {
                sender.sendMessage(translationManager.getMessage("unjail_player_no_longer_jailed").replace("{prefix}", prefix));
            } else {
                plugin.unjailPlayer(targetUUID);
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

                String broadcastTemplate = translationManager.getMessage("unjail_broadcast");
                String broadcastMessage = broadcastTemplate
                        .replace("{prefix}", prefix)
                        .replace("{player}", Objects.requireNonNull(target.getName()));

                if (plugin.getConfig().getBoolean("general.broadcast-on-unjail")) {
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
                } else {
                    String msg = String.format(translationManager.getMessage("unjail_success").replace("{prefix}", prefix), target.getName());
                    sender.sendMessage(msg);
                }
            }
        } else if (command.getName().equalsIgnoreCase("cancelunjail")) {
            sender.sendMessage(prefix + " " + ChatColor.YELLOW + translationManager.getMessage("unjail_operation_canceled"));
        }

        pendingConfirmations.remove(token);
        return true;
    }

    private record UnjailRequest(UUID senderUUID, UUID targetUUID) {
    }
}