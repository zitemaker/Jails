package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class HandcuffListener implements Listener {
    private final JailPlugin plugin;

    public HandcuffListener(JailPlugin plugin){
        this.plugin = plugin;
    }

    private void onItemUse(PlayerInteractEvent event){
        Player player = event.getPlayer();

        if(plugin.getConfig().getBoolean("handcuff-settings.disable-items")){
            event.setCancelled(true);
            player.sendMessage("You cannot use items while handcuffed!");
        }
    }
}
