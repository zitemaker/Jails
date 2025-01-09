package me.zitemaker.jail.utils;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import me.zitemaker.jail.JailPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JailFlagManager {
    private final JailPlugin plugin;
    private final File flagFile;
    private FileConfiguration flagConfig;

    public JailFlagManager(JailPlugin plugin) {
        this.plugin = plugin;
        this.flagFile = new File(plugin.getDataFolder(), "JailFlags.yml");
        loadFlagConfig();
    }

    private void loadFlagConfig() {
        if (!flagFile.exists()) {
            plugin.saveResource("JailFlags.yml", false);
        }
        flagConfig = YamlConfiguration.loadConfiguration(flagFile);
    }

    public void setJailFlag(String name, Location location) {
        String path = "flags." + name;
        flagConfig.set(path + ".world", location.getWorld().getName());
        flagConfig.set(path + ".x", location.getX());
        flagConfig.set(path + ".y", location.getY());
        flagConfig.set(path + ".z", location.getZ());
        flagConfig.set(path + ".yaw", location.getYaw());
        flagConfig.set(path + ".pitch", location.getPitch());

        saveConfig();
    }

    public Location getJailFlag(String name) {
        String path = "flags." + name;
        if (!flagConfig.contains(path)) {
            return null;
        }

        return new Location(
                plugin.getServer().getWorld(flagConfig.getString(path + ".world")),
                flagConfig.getDouble(path + ".x"),
                flagConfig.getDouble(path + ".y"),
                flagConfig.getDouble(path + ".z"),
                (float) flagConfig.getDouble(path + ".yaw"),
                (float) flagConfig.getDouble(path + ".pitch")
        );
    }

    public List<String> getExistingFlagNames() {
        if (!flagConfig.contains("flags")) {
            return new ArrayList<>();
        }

        Set<String> keys = flagConfig.getConfigurationSection("flags").getKeys(false);
        return new ArrayList<>(keys);
    }

    private void saveConfig() {
        try {
            flagConfig.save(flagFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save jail flags to " + flagFile);
            e.printStackTrace();
        }
    }
}