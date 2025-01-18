package me.zitemaker.jail;

import me.zitemaker.jail.commands.*;
import me.zitemaker.jail.flags.*;
import me.zitemaker.jail.listeners.*;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.connections.MojangAPI;
import net.skinsrestorer.api.event.EventBus;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinApplier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.CacheStorage;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JailPlugin extends JavaPlugin {
    private File jailedPlayersFile;
    private FileConfiguration jailedPlayersConfig;

    private File jailLocationsFile;
    private FileConfiguration jailLocationsConfig;
    public List<String> blockedCommands;

    private File handcuffedPlayersFile;
    private FileConfiguration handcuffedPlayersConfig;

    private final Map<UUID, String> playerSpawnPreferences = new HashMap<>();


    @Override
    public void onEnable() {
        getLogger().info("Jails has been enabled!");
        saveDefaultConfig();
        createFiles();
        loadJails();
        loadJailedPlayers();
        loadHandcuffedPlayers();

        blockedCommands = getConfig().getStringList("blockedCommands");

        getCommand("jailset").setExecutor(new JailSetCommand(this));
        getCommand("jail").setExecutor(new JailCommand(this));
        getCommand("jail").setTabCompleter(new JailTabCompleter(this));
        getCommand("tempjail").setExecutor(new TempJailCommand(this));
        getCommand("tempjail").setTabCompleter(new TempJailTabCompleter(this));
        getCommand("deljail").setExecutor(new DelJailCommand(this));
        getCommand("deljail").setTabCompleter(new DelJailTabCompleter(this));
        getCommand("jails").setExecutor(new JailsCommand(this));
        getCommand("jailspawn").setExecutor(new JailSpawnCommand(this));
        getCommand("jailspawn").setTabCompleter(new JailSpawnTabCompleter());
        getCommand("unjail").setExecutor(new UnjailCommand(this));
        getCommand("jailduration").setExecutor(new JailDurationCommand(this));
        getCommand("jailsetflag").setExecutor(new SetFlag(this));
        getCommand("jaildelflag").setExecutor(new DelFlag(this));
        getCommand("jailflaglist").setExecutor(new FlagList(this));
        getCommand("handcuff").setExecutor(new Handcuff(this));
        getCommand("handcuffremove").setExecutor(new HandcuffRemove(this));
        getCommand("jailsreload").setExecutor(new ConfigReload(this));

        JailListCommand jailListCommand = new JailListCommand(this);
        getCommand("jailed").setExecutor(jailListCommand);
        getCommand("jailed").setTabCompleter(jailListCommand);

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new JailListeners(this), this);
        getServer().getPluginManager().registerEvents(new CommandBlocker(this), this);
        getServer().getPluginManager().registerEvents(new FlagBoundaryListener(this), this);
    }

    @Override
    public void onDisable() {
        saveJailedPlayersConfig();
        saveJailLocationsConfig();
        saveHandcuffedPlayersConfig();
        getLogger().info("Jails has been disabled!");
    }

    // ------ Jail Locations ------
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

    // --- Handcuffed Players ---
    public void handcuffPlayer(Player player) {
        UUID playerUUID = player.getUniqueId();
        String basePath = playerUUID.toString();
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.05);
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 1, true, false));

        handcuffedPlayersConfig.set(basePath + ".handcuffed", true);
        saveHandcuffedPlayersConfig();

        getLogger().info("Player " + player.getName() + " has been handcuffed.");
    }

    public void unHandcuffPlayer(UUID playerUUID) {
        if (!isPlayerHandcuffed(playerUUID)) {
            getLogger().warning("Player with UUID " + playerUUID + " is not handcuffed.");
            return;
        }

        Player target = Bukkit.getPlayer(playerUUID);

        target.removePotionEffect(PotionEffectType.DARKNESS);
        target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);

        handcuffedPlayersConfig.set(playerUUID.toString(), null);
        saveHandcuffedPlayersConfig();

        getLogger().info("Player with UUID " + playerUUID + " has been unhandcuffed.");
    }


    public FileConfiguration getHandcuffedPlayersConfig() {
        return handcuffedPlayersConfig;
    }

    public void saveHandcuffedPlayersConfig() {
        try {
            handcuffedPlayersConfig.save(handcuffedPlayersFile);
        } catch (IOException e) {
            getLogger().severe("Could not save handcuffed_players.yml!");
        }
    }

    public boolean isPlayerHandcuffed(UUID playerUUID) {
        return handcuffedPlayersConfig.contains(playerUUID.toString());
    }

    public void loadHandcuffedPlayers() {
        for (String key : handcuffedPlayersConfig.getKeys(false)) {
            getLogger().info("Loaded jailed player: " + key);
        }
    }

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

        setPlayerSpawnOption(playerUUID, "world_spawn");
        saveJailedPlayersConfig();

        boolean keepInventory = getConfig().getBoolean("jail.keep-inventory");
        if (!keepInventory) {
            jailedPlayersConfig.set(basePath + ".inventory", player.getInventory().getContents());
            player.getInventory().clear();

            List<Map<?, ?>> jailItems = getConfig().getMapList("jail.jail-items");
            for (Map<?, ?> itemData : jailItems) {
                try {
                    String itemName = itemData.get("item").toString();
                    int amount = Integer.parseInt(itemData.get("amount").toString());

                    Material material = Material.valueOf(itemName.toUpperCase());
                    ItemStack item = new ItemStack(material, amount);

                    HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                    if (!overflow.isEmpty()) {
                        for (ItemStack overflowItem : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid item in config: " + itemData.get("item"));
                } catch (Exception e) {
                    getLogger().warning("Error giving jail items: " + e.getMessage());
                }
            }
        }

        try {

            String targetSkin = "SirMothsho";
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skin set " + targetSkin + " " + player.getName());
            getLogger().info("Executed /skin clear for " + player.getName() + " to reset their skin.");
        } catch (Exception e) {
            getLogger().severe("Failed to reset skin for " + player.getName() + ": " + e.getMessage());
        }


        Location jailLocation = getJail(jailName);
        if (jailLocation != null) {
            player.teleport(jailLocation);
        } else {
            getLogger().warning("Jail location for " + jailName + " not found.");
        }
        Bukkit.getPluginManager().callEvent(new PlayerJailEvent(player));
    }

    public void unjailPlayer(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        String spawnOption = getPlayerSpawnOption(playerUUID);

        if (player == null) {
            jailedPlayersConfig.set(playerUUID.toString() + ".unjailed", true);
            saveJailedPlayersConfig();
            getLogger().info("Offline player has been marked as unjailed.");
            return;
        }

        boolean keepInventory = getConfig().getBoolean("jail.keep-inventory");
        if (!keepInventory) {
            Object savedInventoryObj = jailedPlayersConfig.get(playerUUID.toString() + ".inventory");
            if (savedInventoryObj instanceof ItemStack[]) {
                player.getInventory().setContents((ItemStack[]) savedInventoryObj);
            } else if (savedInventoryObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<ItemStack> savedInventory = (List<ItemStack>) savedInventoryObj;
                player.getInventory().setContents(savedInventory.toArray(new ItemStack[0]));
            }
        }

        try {

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skin clear " + player.getName());
            getLogger().info("Executed /skin clear for " + player.getName() + " to reset their skin.");
        } catch (Exception e) {
            getLogger().severe("Failed to reset skin for " + player.getName() + ": " + e.getMessage());
        }


        if ("world_spawn".equals(spawnOption)) {
            Location worldSpawn = player.getWorld().getSpawnLocation();
            player.teleport(worldSpawn);
        } else if ("original_location".equals(spawnOption)) {
            teleportToOriginalLocation(player, playerUUID.toString() + ".original");
        } else {
            Location worldSpawn = player.getWorld().getSpawnLocation();
            player.teleport(worldSpawn);
        }


        jailedPlayersConfig.set(playerUUID.toString(), null);
        saveJailedPlayersConfig();
        getLogger().info("Player " + player.getName() + " has been unjailed and their data removed.");
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

    public void setPlayerSpawnOption(UUID playerUUID, String option) {
        jailedPlayersConfig.set(playerUUID.toString() + ".spawnOption", option);
        saveJailedPlayersConfig();
    }

    public String getPlayerSpawnOption(UUID playerUUID) {
        return jailedPlayersConfig.getString(playerUUID.toString() + ".spawnOption", "original_location");
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
        handcuffedPlayersFile = new File(getDataFolder(), "handcuffed_players.yml");
        if (!handcuffedPlayersFile.exists()) {
            try {
                handcuffedPlayersFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create handcuffed_players.yml!");
            }
        }
        handcuffedPlayersConfig = YamlConfiguration.loadConfiguration(handcuffedPlayersFile);

    }

    public FileConfiguration getFlagsConfig() {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), "flags.yml"));
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
            if(player.isOnline()){
                unjailPlayer(player.getUniqueId());
                Bukkit.broadcastMessage(ChatColor.GREEN + player.getName() + " has been unjailed.");
            }
            else if (isPlayerJailed(player.getUniqueId())) {
                Bukkit.broadcastMessage(ChatColor.GREEN + player.getName() + " has been unjailed.");
            }
        }, duration / 50);
    }
}