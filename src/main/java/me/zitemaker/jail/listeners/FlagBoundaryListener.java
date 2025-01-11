package me.zitemaker.jail.listeners;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.io.File;

public class FlagBoundaryListener implements Listener {

    private final JailPlugin plugin;
    private final File flagsFile;
    private final FileConfiguration flagsConfig;

    public FlagBoundaryListener(JailPlugin plugin) {
        this.plugin = plugin;

        this.flagsFile = new File(plugin.getDataFolder(), "flags.yml");
        if (!flagsFile.exists()) {
            try {
                flagsFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().severe("Could not create flags.yml!");
                e.printStackTrace();
            }
        }
        this.flagsConfig = YamlConfiguration.loadConfiguration(flagsFile);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("jail.role.jailed")) {
            return;
        }

        for (String flagName : flagsConfig.getKeys(false)) {
            String worldName = flagsConfig.getString(flagName + ".world");
            String pos1 = flagsConfig.getString(flagName + ".pos1");
            String pos2 = flagsConfig.getString(flagName + ".pos2");

            if (worldName == null || pos1 == null || pos2 == null) continue;

            String[] pos1Coords = pos1.split(",");
            String[] pos2Coords = pos2.split(",");
            if (pos1Coords.length != 3 || pos2Coords.length != 3) continue;

            try {
                double x1 = Math.min(Double.parseDouble(pos1Coords[0]), Double.parseDouble(pos2Coords[0]));
                double y1 = Math.min(Double.parseDouble(pos1Coords[1]), Double.parseDouble(pos2Coords[1]));
                double z1 = Math.min(Double.parseDouble(pos1Coords[2]), Double.parseDouble(pos2Coords[2]));

                double x2 = Math.max(Double.parseDouble(pos1Coords[0]), Double.parseDouble(pos2Coords[0]));
                double y2 = Math.max(Double.parseDouble(pos1Coords[1]), Double.parseDouble(pos2Coords[1]));
                double z2 = Math.max(Double.parseDouble(pos1Coords[2]), Double.parseDouble(pos2Coords[2]));

                if (player.getWorld().getName().equals(worldName)) {
                    double px = player.getLocation().getX();
                    double py = player.getLocation().getY();
                    double pz = player.getLocation().getZ();

                    if (!(px >= x1 && px <= x2 && py >= y1 && py <= y2 && pz >= z1 && pz <= z2)) {
                        Bukkit.broadcastMessage(ChatColor.RED + "[ALERT] A Criminal has recently attempted JailBreak!");
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().severe("Invalid coordinates for flag: " + flagName);
            }
        }
    }
}