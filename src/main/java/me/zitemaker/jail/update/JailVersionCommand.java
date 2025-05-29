package me.zitemaker.jail.update;

import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.listeners.TranslationManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class JailVersionCommand implements CommandExecutor {
    private final JailPlugin plugin;
    private final TranslationManager translationManager;
    private final String prefix;
    private final UpdateChecker updateChecker;
    private static final int SPIGOTMC_RESOURCE_ID = 123183;

    public JailVersionCommand(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
        this.prefix = plugin.getPrefix();
        this.updateChecker = new UpdateChecker(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(prefix + " " + ChatColor.GRAY + translationManager.getMessage("version_checking"));

        updateChecker.fetchRemoteVersion().thenAccept(remoteVersion -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                String currentVersion = plugin.getDescription().getVersion().trim().replace("v", "");
                if (remoteVersion == null) {
                    sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("version_could_not_determine"));
                    return;
                }
                String normalizedRemote = remoteVersion.trim().replace("v", "");

                if (normalizedRemote.equals(currentVersion)) {
                    String message = translationManager.getMessage("version_latest");
                    String formatted = String.format(message, ChatColor.AQUA + currentVersion);
                    sender.sendMessage(prefix + " " + ChatColor.GREEN + formatted);
                } else {
                    String newAvailable = translationManager.getMessage("version_new_available");
                    String formattedNew = String.format(newAvailable, ChatColor.AQUA + normalizedRemote);
                    sender.sendMessage(prefix + " " + ChatColor.YELLOW + formattedNew);

                    String current = translationManager.getMessage("version_current");
                    String formattedCurrent = String.format(current, ChatColor.RED + currentVersion);
                    sender.sendMessage(prefix + " " + ChatColor.YELLOW + formattedCurrent);

                    String url = "https://www.spigotmc.org/resources/" + SPIGOTMC_RESOURCE_ID + "/";
                    String download = translationManager.getMessage("version_download");
                    String formattedDownload = String.format(download, ChatColor.UNDERLINE + url);
                    sender.sendMessage(prefix + " " + ChatColor.GOLD + formattedDownload);
                }
            });
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(prefix + " " + ChatColor.RED + translationManager.getMessage("version_check_failed"));
            });
            plugin.getLogger().warning("Update check failed: " + ex.getMessage());
            return null;
        });

        return true;
    }
}