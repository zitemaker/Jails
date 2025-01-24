package me.zitemaker.jail;

import me.zitemaker.jail.commands.*;
import me.zitemaker.jail.flags.*;
import me.zitemaker.jail.listeners.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JailPlugin extends JavaPlugin {
    private File jailedPlayersFile;
    private FileConfiguration jailedPlayersConfig;

    private File jailLocationsFile;
    private FileConfiguration jailLocationsConfig;
    public List<String> blockedCommands;
    private final Map<UUID, Long> alertCooldown = new HashMap<>();
    private File handcuffedPlayersFile;
    private FileConfiguration handcuffedPlayersConfig;
    private static final long COOLDOWN_TIME = 5000;
    private File flagsFile;
    private FileConfiguration flagsConfig;

    private final Map<UUID, String> playerSpawnPreferences = new HashMap<>();
    private final Set<UUID> alreadyAlerted = new HashSet<>();
    private final Set<String> notifiedInsecureJails = new HashSet<>();
    public final String prefix = getConfig().getString("prefix", "&7[&eJails&7]");
    public final boolean alertMessages = getConfig().getBoolean("jail-settings.enable-escape-alerts");

    @Override
    public void onEnable() {
        getLogger().info("Jails has been enabled!");
        saveDefaultConfig();
        createFiles();
        loadJails();
        loadJailedPlayers();
        loadHandcuffedPlayers();
        initializeFlagsFile();
        reloadFlagsConfig();

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
        getServer().getPluginManager().registerEvents(new HandcuffListener(this), this);
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
        if(getConfig().getBoolean("handcuff-settings.slow-movement")){
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.05);
        }
        if(getConfig().getBoolean("handcuff-settings.blindness")){
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 1, true, false));
        }

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

    // --- Jailed Players ---

    public void jailPlayer(Player player, String jailName, long endTime, String reason, String jailer) {
        Location jailLocation = getJail(jailName);

        if (jailLocation != null && !isLocationInAnyFlag(jailLocation)) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[SECURITY BREACH] " +
                    ChatColor.GOLD + "Prison Security Warning: " + ChatColor.RED +
                    "Player " + ChatColor.YELLOW + player.getName() + ChatColor.RED +
                    " is being jailed in '" + ChatColor.YELLOW + jailName + ChatColor.RED +
                    "' which is not within any security zone (flag)!");

            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("jailplugin.admin")) {
                    staff.sendMessage(ChatColor.RED + "[SECURITY ALERT] " +
                            ChatColor.GOLD + "Warning: " + ChatColor.RED +
                            "Jail '" + jailName + "' is not secured within a flag zone!");
                }
            }
        }

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

        switch(getConfig().getString("general.default-unjail-location")){

            case "WORLD_SPAWN":
                setPlayerSpawnOption(playerUUID, "world_spawn");
                break;

            case "ORIGINAL_LOCATION":
                setPlayerSpawnOption(playerUUID, "original_location");
                break;

            default:
                setPlayerSpawnOption(playerUUID, "world_spawn");
                break;
        }

        boolean keepInventory = getConfig().getBoolean("jail-settings.keep-inventory");
        if (!keepInventory) {
            jailedPlayersConfig.set(basePath + ".inventory", player.getInventory().getContents());
            player.getInventory().clear();

            List<Map<?, ?>> jailItems = getConfig().getMapList("jail-settings.jail-items");
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
            saveJailedPlayersConfig();
        }

        if(getConfig().getBoolean("jail-settings.change-skin")){
            try {
                String targetSkin = getConfig().getString("jail-settings.skin-username", "SirMothsho");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skin set " + targetSkin + " " + player.getName());
            } catch (Exception e) {
                getLogger().severe("Failed to set skin for " + player.getName() + ": " + e.getMessage());
            }
        }


        if (jailLocation != null) {
            player.teleport(jailLocation);
        } else {
            getLogger().warning("Jail location for " + jailName + " not found.");
        }
        Bukkit.getPluginManager().callEvent(new PlayerJailEvent(player));
    }

    public boolean isLocationInAnyFlag(Location location) {
        FileConfiguration flagsConfig = getFlagsConfig();

        for (String flagName : flagsConfig.getKeys(false)) {
            String worldName = flagsConfig.getString(flagName + ".world");
            if (worldName == null) continue;

            String pos1String = flagsConfig.getString(flagName + ".pos1");
            String pos2String = flagsConfig.getString(flagName + ".pos2");
            if (pos1String == null || pos2String == null) continue;

            try {
                String[] pos1 = pos1String.split(",");
                String[] pos2 = pos2String.split(",");

                if (!location.getWorld().getName().equals(worldName)) continue;

                int minX = Math.min(Integer.parseInt(pos1[0]), Integer.parseInt(pos2[0]));
                int minY = Math.min(Integer.parseInt(pos1[1]), Integer.parseInt(pos2[1]));
                int minZ = Math.min(Integer.parseInt(pos1[2]), Integer.parseInt(pos2[2]));
                int maxX = Math.max(Integer.parseInt(pos1[0]), Integer.parseInt(pos2[0]));
                int maxY = Math.max(Integer.parseInt(pos1[1]), Integer.parseInt(pos2[1]));
                int maxZ = Math.max(Integer.parseInt(pos1[2]), Integer.parseInt(pos2[2]));

                if (location.getX() >= minX && location.getX() <= maxX &&
                        location.getY() >= minY && location.getY() <= maxY &&
                        location.getZ() >= minZ && location.getZ() <= maxZ) {
                    return true;
                }
            } catch (Exception e) {
                getLogger().warning("Invalid flag coordinates for flag '" + flagName + "': " + e.getMessage());
                continue;
            }
        }
        return false;
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

        boolean keepInventory = getConfig().getBoolean("jail-settings.keep-inventory");
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

        if(getConfig().getBoolean("jail-settings.change-skin")){
            try {

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skin clear " + player.getName());
                getLogger().info("Executed /skin clear for " + player.getName() + " to reset their skin.");
            } catch (Exception e) {
                getLogger().severe("Failed to reset skin for " + player.getName() + ": " + e.getMessage());
            }
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

    /*public void playerEscape(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        String spawnOption = getPlayerSpawnOption(playerUUID);

        if (player == null) {
            jailedPlayersConfig.set(playerUUID.toString() + ".unjailed", true);
            saveJailedPlayersConfig();
            getLogger().info("Offline player has escaped.");
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

        jailedPlayersConfig.set(playerUUID.toString(), null);
        saveJailedPlayersConfig();
        getLogger().info("Player " + player.getName() + " has escaped from jail.");
    }
     */

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

    // --- Flags ---

    public boolean isPlayerInsideFlag(Location location, FileConfiguration flagsConfig, String flagName) {
        String worldName = flagsConfig.getString(flagName + ".world");
        String pos1String = flagsConfig.getString(flagName + ".pos1");
        String pos2String = flagsConfig.getString(flagName + ".pos2");

        if (worldName == null || pos1String == null || pos2String == null) return false;
        if (!location.getWorld().getName().equals(worldName)) return false;

        try {
            String[] pos1 = pos1String.split(",");
            String[] pos2 = pos2String.split(",");

            int minX = Math.min(Integer.parseInt(pos1[0]), Integer.parseInt(pos2[0]));
            int minY = Math.min(Integer.parseInt(pos1[1]), Integer.parseInt(pos2[1]));
            int minZ = Math.min(Integer.parseInt(pos1[2]), Integer.parseInt(pos2[2]));
            int maxX = Math.max(Integer.parseInt(pos1[0]), Integer.parseInt(pos2[0]));
            int maxY = Math.max(Integer.parseInt(pos1[1]), Integer.parseInt(pos2[1]));
            int maxZ = Math.max(Integer.parseInt(pos1[2]), Integer.parseInt(pos2[2]));

            return location.getX() >= minX && location.getX() <= maxX &&
                    location.getY() >= minY && location.getY() <= maxY &&
                    location.getZ() >= minZ && location.getZ() <= maxZ;
        } catch (Exception e) {
            getLogger().warning("Invalid flag coordinates for flag '" + flagName + "': " + e.getMessage());
            return false;
        }
    }

    public void handleTeleportBack(Player player, UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        if (!alertCooldown.containsKey(playerUUID) || currentTime - alertCooldown.get(playerUUID) > COOLDOWN_TIME) {
            String jailName = getJailedPlayersConfig().getString(playerUUID.toString() + ".jailName");
            Location jailLocation = jailName != null ? getJail(jailName) : null;

            if (jailLocation != null && isLocationInAnyFlag(jailLocation)) {
                player.teleport(jailLocation);
                player.sendMessage(ChatColor.YELLOW + "You were teleported back for trying to escape from jail.");
                if(alertMessages){
                    Bukkit.broadcastMessage(ChatColor.DARK_RED + "[Security Alert] " +
                            ChatColor.GOLD + player.getName() + ChatColor.RED + " attempted to escape and was returned.");
                }
            } else {

            }

            alertCooldown.put(playerUUID, currentTime);
        }
    }


    // --- Flags ---

    public void handleEscape(Player player, UUID playerUUID) {
        if (!alreadyAlerted.contains(playerUUID)) {
            String jailName = getJailedPlayersConfig().getString(playerUUID.toString() + ".jailName");
            if (jailName != null) {
                Location jailLocation = getJail(jailName);
                if (jailLocation != null && isLocationInAnyFlag(jailLocation)) {
                    Location escapeLocation = player.getLocation();

                    String originalSpawnOption = getJailedPlayersConfig().getString(
                            playerUUID.toString() + ".spawnOption");

                    getJailedPlayersConfig().set(playerUUID.toString() + ".spawnOption", "none");
                    saveJailedPlayersConfig();



                    boolean punishmentFeature = getConfig().getBoolean("jail-settings.punish-on-escape");

                    if (getConfig().getBoolean("jail-settings.punish-on-escape")) {
                        String punishment = getConfig().getString("jail-settings.escape-punishment");
                        switch (punishment) {
                            case "KILL":
                                player.setHealth(0.0);
                                String killMessageTemplate = getConfig().getString("jail-settings.escape-punishment-message",
                                        "{prefix} &cYou were punished for trying to escape jail!");
                                String killPunishmentMessage = killMessageTemplate.replace("{prefix}", getPrefix());
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', killPunishmentMessage));
                                break;

                            case "TELEPORT_BACK":
                                /*if (jailLocation != null && isLocationInAnyFlag(jailLocation)) {
                                    player.teleport(jailLocation);
                                    player.sendMessage(ChatColor.YELLOW + "You were teleported back for trying to escape from jail.");
                                    alreadyAlerted.remove(playerUUID);
                                }
                                 */
                                handleTeleportBack(player, playerUUID);

                                break;

                            case "BAN":
                                String banMessageTemplate = getConfig().getString("jail-settings.escape-punishment-message",
                                        "{prefix} &cYou were punished for trying to escape jail!");
                                String banDurationConfig = getConfig().getString("jail-settings.ban-duration", "1d");


                                long banDurationMillis = parseDuration(banDurationConfig);
                                Date banExpiry = banDurationMillis > 0 ? new Date(System.currentTimeMillis() + banDurationMillis) : null;

                                String banPunishmentMessage = banMessageTemplate.replace("{prefix}", getPrefix());
                                Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(),
                                        ChatColor.translateAlternateColorCodes('&', banPunishmentMessage),
                                        banExpiry,
                                        "CONSOLE");

                                player.kickPlayer(ChatColor.translateAlternateColorCodes('&', banPunishmentMessage));
                                break;

                            default:
                                Bukkit.getLogger().warning("Unknown punishment type in config: " + punishment);
                                break;
                        }
                    }

                    if(alertMessages){
                        Bukkit.broadcastMessage(ChatColor.RED + "[Alert] " + ChatColor.GOLD + player.getName() +
                                ChatColor.RED + " has escaped from jail! Security breach detected!");
                    }


                } else {
                }
            }
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

        flagsFile = new File(getDataFolder(), "flags.yml");
        if(!flagsFile.exists()){
            try{
                flagsFile.createNewFile();
            } catch (IOException e){
                getLogger().severe("Could not create handcuffed_players.yml");
            }

        }
        flagsConfig = YamlConfiguration.loadConfiguration(flagsFile);

    }
    public void handleBoundaryCheck(Player player, UUID playerUUID) {
        FileConfiguration flagsConfig = getFlagsConfig();
        String assignedFlag = getJailedPlayersConfig().getString(playerUUID.toString() + ".assignedFlag");
        boolean isInsideAssignedFlag = false;

        if (assignedFlag != null && isPlayerInsideFlag(player.getLocation(), flagsConfig, assignedFlag)) {
            isInsideAssignedFlag = true;
        } else {
            for (String flagName : flagsConfig.getKeys(false)) {
                if (isPlayerInsideFlag(player.getLocation(), flagsConfig, flagName)) {
                    isInsideAssignedFlag = true;
                    getJailedPlayersConfig().set(playerUUID.toString() + ".assignedFlag", flagName);
                    saveJailedPlayersConfig();
                    break;
                }
            }
        }

        if (!isInsideAssignedFlag) {
            handleEscape(player, playerUUID);
            /*if (shouldTeleport) {
                handleTeleportBack(player, playerUUID);
            } else {
                handleEscape(player, playerUUID);
            }

             */
        }
    }

    public void notifyInsecureJail(String jailName, Location jailLocation, Player setter) {
        if (!isLocationInAnyFlag(jailLocation) && !notifiedInsecureJails.contains(jailName)) {
            notifiedInsecureJails.add(jailName);

                setter.sendMessage(ChatColor.RED + "[SECURITY ALERT] The jail '" + jailName +
                        "' is not within a secure flag zone!");

                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[SECURITY ALERT] Jail '" + jailName +
                        "' is not secured within a flagged zone! Prisoner escapes won't be detected!");

        }
    }

    public FileConfiguration getFlagsConfig() {
        return flagsConfig;
    }


    public void saveFlagsConfig() {
        try {
            flagsConfig.save(flagsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save flags.yml!");
        }
    }

    public void initializeFlagsFile() {
        if (!flagsFile.exists()) {
            getLogger().info("flags.yml does not exist, creating new file...");
            try {
                getDataFolder().mkdirs();
                flagsFile.createNewFile();
                getLogger().info("Successfully created flags.yml");
            } catch (IOException e) {
                getLogger().severe("Failed to create flags.yml!");
                e.printStackTrace();
            }
        }

        reloadFlagsConfig();
    }

    public void reloadFlagsConfig() {
        this.flagsConfig = YamlConfiguration.loadConfiguration(flagsFile);
        getLogger().info("Loaded flags.yml with " + flagsConfig.getKeys(false).size() + " flags");

        Set<String> flags = flagsConfig.getKeys(false);
        if (flags.isEmpty()) {
            getLogger().warning("No flags found in flags.yml");
        } else {
            getLogger().info("Found flags:");
            for (String flag : flags) {
                getLogger().info("- " + flag + ":");
                getLogger().info("  World: " + flagsConfig.getString(flag + ".world"));
                getLogger().info("  Pos1: " + flagsConfig.getString(flag + ".pos1"));
                getLogger().info("  Pos2: " + flagsConfig.getString(flag + ".pos2"));
            }
        }
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
        String prefix = getPrefix();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if(player.isOnline()){
                unjailPlayer(player.getUniqueId());
                String messageTemplate = getConfig().getString("general.unjail-broadcast-message",
                        "{prefix} &c{player} has been unjailed.");

                String broadcastMessage = messageTemplate
                        .replace("{prefix}", ChatColor.translateAlternateColorCodes('&', prefix))
                        .replace("{player}", player.getName());

                if(getConfig().getBoolean("general.broadcast-on-unjail")){
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
                }
            }
            else if (isPlayerJailed(player.getUniqueId())) {
                String messageTemplate = getConfig().getString("general.unjail-broadcast-message",
                        "{prefix} &c{player} has been unjailed.");

                String broadcastMessage = messageTemplate
                        .replace("{prefix}", ChatColor.translateAlternateColorCodes('&', prefix))
                        .replace("{player}", player.getName());

                if(getConfig().getBoolean("general.broadcast-on-unjail")){
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
                }
            }
        }, duration / 50);
    }

    public String getPrefix(){
        return prefix;
    }

    public String formatTimeLeft(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + " second" + (seconds == 1 ? "" : "s");

        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " minute" + (minutes == 1 ? "" : "s");

        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s");

        long days = hours / 24;
        return days + " day" + (days == 1 ? "" : "s");
    }

    public Set<UUID> getAlreadyAlerted(){
        return alreadyAlerted;
    }

    public Map<UUID, Long>getAlertCooldown(){
        return alertCooldown;
    }
}