package me.zitemaker.jail.update;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class JailVersionCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final UpdateChecker updateChecker;
    private static final int SPIGOTMC_RESOURCE_ID = 123183;

    public JailVersionCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.updateChecker = new UpdateChecker(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("§7Checking for latest version...");
        updateChecker.fetchRemoteVersion().thenAccept(remoteVersion -> {
            String currentVersion = plugin.getDescription().getVersion().trim().replace("v", "");
            if (remoteVersion == null) {
                sender.sendMessage("§cCould not determine latest version.");
                return;
            }
            String normalizedRemote = remoteVersion.trim().replace("v", "");

            if (normalizedRemote.equals(currentVersion)) {
                sender.sendMessage("§aYou are using the latest version: §b" + currentVersion);
            } else {
                sender.sendMessage("§eA new version is available: §b" + normalizedRemote);
                sender.sendMessage("§eYou are using: §c" + currentVersion);
                sender.sendMessage("§6Download the latest version: §nhttps://www.spigotmc.org/resources/" + SPIGOTMC_RESOURCE_ID + "/");
            }
        }).exceptionally(ex -> {
            sender.sendMessage("§cFailed to check for updates.");
            plugin.getLogger().warning("Update check failed: " + ex.getMessage());
            return null;
        });
        return true;
    }
}