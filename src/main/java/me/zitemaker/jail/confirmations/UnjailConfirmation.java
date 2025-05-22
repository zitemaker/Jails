package me.zitemaker.jail.confirmations;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class UnjailConfirmation implements CommandExecutor {

    private final JailPlugin plugin;
    private final TranslationManager translationManager;
    private final String prefix;
    private final Map<String, UnjailRequest> pendingConfirmations = new ConcurrentHashMap<>();

    public UnjailConfirmation(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
        this.prefix = plugin.getPrefix();
    }

    public String generateToken(UUID senderUUID, UUID targetUUID, boolean isIpJailed) {
        String token = UUID.randomUUID().toString();
        pendingConfirmations.put(token, new UnjailRequest(senderUUID, targetUUID, isIpJailed));
        return token;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + ChatColor.RED + translationManager.getMessage("confirmation_only_players"));
            return true;
        }
        if (args.length != 1) {
            String usage = String.format(translationManager.getMessage("confirmation_usage"), label);
            sender.sendMessage(prefix + ChatColor.RED + usage);
            return true;
        }

        String token = args[0];
        UnjailRequest request = pendingConfirmations.get(token);

        if (request == null) {
            sender.sendMessage(prefix + ChatColor.RED + translationManager.getMessage("confirmation_invalid_token"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.getUniqueId().equals(request.getSenderUUID())) {
            sender.sendMessage(prefix + ChatColor.RED + translationManager.getMessage("confirmation_not_authorized"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("confirmunjail")) {
            UUID targetUUID = request.getTargetUUID();
            boolean isIpJailed = request.isIpJailed();
            if (!plugin.isPlayerJailed(targetUUID)) {
                sender.sendMessage(prefix + ChatColor.YELLOW + translationManager.getMessage("unjail_player_no_longer_jailed"));
            } else {
                plugin.unjailPlayer(targetUUID);
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
                boolean wasIpJailed = false;

                if (isIpJailed) {
                    Player targetPlayer = target.getPlayer();
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        wasIpJailed = plugin.removePlayerIpJail(targetPlayer);
                    }
                }

                String broadcastTemplate = translationManager.getMessage("unjail_broadcast");
                String broadcastMessage = broadcastTemplate
                        .replace("{prefix}", prefix)
                        .replace("{player}", target.getName());

                if (plugin.getConfig().getBoolean("general.broadcast-on-unjail")) {
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
                } else {
                    if (wasIpJailed) {
                        String msg = String.format(translationManager.getMessage("unjail_success_ip_removed"), target.getName());
                        sender.sendMessage(prefix + ChatColor.GREEN + msg);
                    } else {
                        String msg = String.format(translationManager.getMessage("unjail_success"), target.getName());
                        sender.sendMessage(prefix + ChatColor.GREEN + msg);
                    }
                }
            }
        } else if (command.getName().equalsIgnoreCase("cancelunjail")) {
            sender.sendMessage(prefix + ChatColor.YELLOW + translationManager.getMessage("unjail_operation_canceled"));
        }

        pendingConfirmations.remove(token);
        return true;
    }

    private static class UnjailRequest {
        private final UUID senderUUID;
        private final UUID targetUUID;
        private final boolean isIpJailed;

        public UnjailRequest(UUID senderUUID, UUID targetUUID, boolean isIpJailed) {
            this.senderUUID = senderUUID;
            this.targetUUID = targetUUID;
            this.isIpJailed = isIpJailed;
        }

        public UUID getSenderUUID() {
            return senderUUID;
        }

        public UUID getTargetUUID() {
            return targetUUID;
        }

        public boolean isIpJailed() {
            return isIpJailed;
        }
    }
}