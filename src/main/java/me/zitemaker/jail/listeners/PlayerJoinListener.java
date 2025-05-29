package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import me.zitemaker.jail.listeners.TranslationManager;

public class PlayerJoinListener implements Listener {
    private final JailPlugin plugin;
    private final TranslationManager translationManager;

    public PlayerJoinListener(JailPlugin plugin) {
        this.plugin = plugin;
        this.translationManager = plugin.getTranslationManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerIp = player.getAddress().getAddress().getHostAddress();
        String hashedIp = plugin.hashIpAddress(playerIp);

        if (plugin.isIpJailed(hashedIp) && !plugin.isPlayerJailed(player.getUniqueId())) {
            JailPlugin.JailedIpInfo info = plugin.getJailedIpInfo(hashedIp);

            if (info != null) {
                long duration = info.getReleaseTime() > 0 ?
                        info.getReleaseTime() - System.currentTimeMillis() :
                        -1;

                plugin.jailPlayer(
                        player,
                        info.getJailName(),
                        duration,
                        translationManager.getMessage("ipjail_ip_association") + info.getReason() + info.getReason(),
                        info.getJailer()
                );

                player.sendMessage(plugin.getPrefix() + " " +
                        ChatColor.RED + translationManager.getMessage("ip_jail_autojail"));
            }
        }
    }
}