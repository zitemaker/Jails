package me.zitemaker.jail.confirmations;

import me.zitemaker.jail.JailPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class UnjailConfirmation implements CommandExecutor {

    private final JailPlugin plugin;
    private final Map<String, UnjailRequest> pendingConfirmations = new ConcurrentHashMap<>();

    public UnjailConfirmation(JailPlugin plugin) {
        this.plugin = plugin;
    }

    public String generateToken(UUID senderUUID, UUID targetUUID) {
        String token = UUID.randomUUID().toString();
        pendingConfirmations.put(token, new UnjailRequest(senderUUID, targetUUID));
        return token;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Usage: " + org.bukkit.ChatColor.YELLOW + "/" + label + " <token>");
            return true;
        }

        String token = args[0];
        UnjailRequest request = pendingConfirmations.get(token);

        if (request == null) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Invalid or expired token.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.getUniqueId().equals(request.getSenderUUID())) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "You are not authorized to confirm this action.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("confirmunjail")) {
            UUID targetUUID = request.getTargetUUID();
            if (!plugin.isPlayerJailed(targetUUID)) {
                sender.sendMessage(org.bukkit.ChatColor.YELLOW + "Player is no longer jailed.");
            } else {
                plugin.unjailPlayer(targetUUID);
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

                String prefix = plugin.getPrefix();
                String messageTemplate = plugin.getConfig().getString("general.unjail-broadcast-message",
                        "{prefix} &c{player} has been unjailed.");
                String broadcastMessage = messageTemplate
                        .replace("{prefix}", org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix))
                        .replace("{player}", target.getName());

                if (plugin.getConfig().getBoolean("general.broadcast-on-unjail")) {
                    Bukkit.broadcastMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', broadcastMessage));
                } else {
                    sender.sendMessage(org.bukkit.ChatColor.GREEN + "Player " + target.getName() + " has been unjailed.");
                }
            }
        } else if (command.getName().equalsIgnoreCase("cancelunjail")) {
            sender.sendMessage(org.bukkit.ChatColor.YELLOW + "Unjail operation canceled.");
        }

        pendingConfirmations.remove(token);
        return true;
    }

    private static class UnjailRequest {
        private final UUID senderUUID;
        private final UUID targetUUID;

        public UnjailRequest(UUID senderUUID, UUID targetUUID) {
            this.senderUUID = senderUUID;
            this.targetUUID = targetUUID;
        }

        public UUID getSenderUUID() {
            return senderUUID;
        }

        public UUID getTargetUUID() {
            return targetUUID;
        }
    }
}