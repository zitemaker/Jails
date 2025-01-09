package me.zitemaker.jail;

import me.zitemaker.jail.commands.*;
import me.zitemaker.jail.listeners.ChatListener;
import me.zitemaker.jail.listeners.CommandBlocker;
import me.zitemaker.jail.listeners.JailListeners;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JailPlugin extends JavaPlugin {
    private File jailedPlayersFile;
    private FileConfiguration jailedPlayersConfig;

    private File jailLocationsFile;
    private FileConfiguration jailLocationsConfig;
    public List<String> blockedCommands;

    @Override
    public void onEnable() {
        getLogger().info("JailPlugin has been enabled");
        saveDefaultConfig();
        createFiles();
        loadJails();
        loadJailedPlayers();

        blockedCommands = getConfig().getStringList("blockedCommands");


        getCommand("jailset").setExecutor(new JailSetCommand(this));
        getCommand("jail").setExecutor(new JailCommand(this));
        getCommand("tempjail").setExecutor(new TempJailCommand(this));
        getCommand("jails").setExecutor(new JailsCommand(this));
        getCommand("unjail").setExecutor(new UnjailCommand(this));
        getCommand("jailduration").setExecutor(new JailDurationCommand(this));


        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new JailListeners(this), this);
        getServer().getPluginManager().registerEvents(new CommandBlocker(this), this);
    }

    @Override
    public void onDisable() {
        saveJailedPlayersConfig();
        saveJailLocationsConfig();
        getLogger().info("JailPlugin has been disabled!");
    }

    // --- Jail Locations ---
    public void addJail(String name, Location location) {
        jailLocationsConfig.set(name + ".world", location.getWorld().getName());
        jailLocationsConfig.set(name + ".x", location.getX());
        jailLocationsConfig.set(name + ".y", location.getY());
        jailLocationsConfig.set(name + ".z", location.getZ());
        jailLocationsConfig.set(name + ".yaw", location.getYaw());
        jailLocationsConfig.set(name + ".pitch", location.getPitch());
        saveJailLocationsConfig();
    }

    public Location getJail(String name) {
        if (!jailLocationsConfig.contains(name)) {
            return null;
        }

        String worldName = jailLocationsConfig.getString(name + ".world");
        double x = jailLocationsConfig.getDouble(name + ".x");
        double y = jailLocationsConfig.getDouble(name + ".y");
        double z = jailLocationsConfig.getDouble(name + ".z");
        float yaw = (float) jailLocationsConfig.getDouble(name + ".yaw");
        float pitch = (float) jailLocationsConfig.getDouble(name + ".pitch");

        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    public Map<String, Location> getJails() {
        Map<String, Location> jails = new HashMap<>();
        for (String key : jailLocationsConfig.getKeys(false)) {
            Location location = getJail(key);
            if (location != null) {
                jails.put(key, location);
            }
        }
        return jails;
    }

    public void removeJail(String name) {
        jailLocationsConfig.set(name, null);
        saveJailLocationsConfig();
    }

    public void loadJails() {
        for (String key : jailLocationsConfig.getKeys(false)) {
            getLogger().info("Loaded jail: " + key);
        }
    }

    public FileConfiguration getJailLocationsConfig() {
        return jailLocationsConfig;
    }

    public void saveJailLocationsConfig() {
        try {
            jailLocationsConfig.save(jailLocationsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save jail_locations.yml!");
        }
    }

    // --- Jailed Players ---
    public void jailPlayer(Player player, String jailName, long endTime, String reason, String jailer) {
        UUID playerUUID = player.getUniqueId();
        String basePath = playerUUID.toString();


        Location originalLocation = player.getLocation();
        jailedPlayersConfig.set(basePath + ".original.world", originalLocation.getWorld().getName());
        jailedPlayersConfig.set(basePath + ".original.x", originalLocation.getX());
        jailedPlayersConfig.set(basePath + ".original.y", originalLocation.getY());
        jailedPlayersConfig.set(basePath + ".original.z", originalLocation.getZ());
        jailedPlayersConfig.set(basePath + ".original.yaw", originalLocation.getYaw());
        jailedPlayersConfig.set(basePath + ".original.pitch", originalLocation.getPitch());


        jailedPlayersConfig.set(basePath + ".jailName", jailName);
        jailedPlayersConfig.set(basePath + ".endTime", endTime);
        jailedPlayersConfig.set(basePath + ".reason", reason);
        jailedPlayersConfig.set(basePath + ".jailer", jailer);

        saveJailedPlayersConfig();


        Location jailLocation = getJail(jailName);
        if (jailLocation != null) {
            player.teleport(jailLocation);
        } else {
            getLogger().warning("Jail location for " + jailName + " not found.");
        }
    }



    public void unjailPlayer(UUID playerUUID) {
        String basePath = playerUUID.toString() + ".original";

        if (jailedPlayersConfig.contains(basePath)) {

            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                teleportToOriginalLocation(player, basePath);
            } else {
                getLogger().info("Player is offline. Their location will be restored upon next login.");
            }


            jailedPlayersConfig.set(playerUUID.toString(), null);
            saveJailedPlayersConfig();
        } else {
            getLogger().warning("No original location found for player with UUID: " + playerUUID);
        }
    }



    public void teleportToOriginalLocation(Player player, String basePath) {
        String worldName = jailedPlayersConfig.getString(basePath + ".world");
        if (worldName == null || Bukkit.getWorld(worldName) == null) {
            getLogger().warning("Could not teleport player " + player.getName() + " back to their original location. World '" + worldName + "' not found.");
            return;
        }

        double x = jailedPlayersConfig.getDouble(basePath + ".x");
        double y = jailedPlayersConfig.getDouble(basePath + ".y");
        double z = jailedPlayersConfig.getDouble(basePath + ".z");
        float yaw = (float) jailedPlayersConfig.getDouble(basePath + ".yaw");
        float pitch = (float) jailedPlayersConfig.getDouble(basePath + ".pitch");

        Location originalLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
        player.teleport(originalLocation);
        getLogger().info("Teleported player " + player.getName() + " back to their original location.");
    }




    public boolean isPlayerJailed(UUID playerUUID) {
        return jailedPlayersConfig.contains(playerUUID.toString());
    }

    public String getPlayerJail(UUID playerUUID) {
        return jailedPlayersConfig.getString(playerUUID.toString());
    }

    public void loadJailedPlayers() {
        for (String key : jailedPlayersConfig.getKeys(false)) {
            getLogger().info("Loaded jailed player: " + key);
        }
    }

    public FileConfiguration getJailedPlayersConfig() {
        return jailedPlayersConfig;
    }

    public void saveJailedPlayersConfig() {
        try {
            jailedPlayersConfig.save(jailedPlayersFile);
        } catch (IOException e) {
            getLogger().severe("Could not save jailed_players.yml!");
        }
    }

    // --- File Management ---
    private void createFiles() {
        jailedPlayersFile = new File(getDataFolder(), "jailed_players.yml");
        if (!jailedPlayersFile.exists()) {
            saveResource("jailed_players.yml", false);
        }
        jailedPlayersConfig = YamlConfiguration.loadConfiguration(jailedPlayersFile);

        jailLocationsFile = new File(getDataFolder(), "jail_locations.yml");
        if (!jailLocationsFile.exists()) {
            saveResource("jail_locations.yml", false);
        }
        jailLocationsConfig = YamlConfiguration.loadConfiguration(jailLocationsFile);
    }

    // --- Utilities ---
    public static long parseDuration(String input) {
        long time = 0L;
        StringBuilder number = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else if ("smhd".indexOf(c) >= 0) {
                int value = Integer.parseInt(number.toString());
                switch (c) {
                    case 's': time += value * 1000L; break;
                    case 'm': time += value * 60 * 1000L; break;
                    case 'h': time += value * 60 * 60 * 1000L; break;
                    case 'd': time += value * 24 * 60 * 60 * 1000L; break;
                }
                number = new StringBuilder();
            }
        }
        return time;
    }

    public void scheduleUnjail(Player player, long duration) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (isPlayerJailed(player.getUniqueId())) {
                unjailPlayer(player.getUniqueId());
                Bukkit.broadcastMessage(ChatColor.GREEN + player.getName() + " has been unjailed.");
            }
        }, duration / 50);
    }
}
