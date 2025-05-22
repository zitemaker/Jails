/*
 * Jails
 * Copyright (C) 2025 Zitemaker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package me.zitemaker.jail;

import me.zitemaker.jail.commands.*;
import me.zitemaker.jail.listeners.*;
import me.zitemaker.jail.update.*;
import me.zitemaker.jail.utils.*;
import me.zitemaker.jail.confirmations.*;
import me.zitemaker.jail.ipjail.*;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.HashMap;
import java.util.Map;

public class JailPlugin extends JavaPlugin {
    private File jailedPlayersFile;
    private FileConfiguration jailedPlayersConfig;

    private Map<String, JailedIpInfo> jailedIps = new HashMap<>();
    private File ipJailFile;
    private FileConfiguration ipJailConfig;
    private File jailLocationsFile;
    private FileConfiguration jailLocationsConfig;
    public List<String> blockedCommands;
    private final Map<UUID, Long> alertCooldown = new HashMap<>();
    private File handcuffedPlayersFile;
    private FileConfiguration handcuffedPlayersConfig;
    private static final long COOLDOWN_TIME = 5000;

    private final Map<UUID, String> playerSpawnPreferences = new HashMap<>();
    private final Set<UUID> alreadyAlerted = new HashSet<>();
    private final Set<String> notifiedInsecureJails = new HashSet<>();
    public String prefix;
    public boolean alertMessages;
    public String targetSkin;
    public double handcuffSpeed;
    public String purchaseLink = "https://builtbybit.com/resources/jails.62499/";
    public UnjailConfirmation unjailConfirmation;
    private Console console = new SpigotConsole();;
    private PlatformLogger platformLogger;
    private Logger logger = new Logger(new JavaPlatformLogger(console, getLogger()), true);
    private final boolean loggerColor = true;
    private TranslationManager translationManager;
    private Handcuff handcuffInstance;
    private Handcuff handcuffCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        createFiles();
        loadJails();
        loadJailedPlayers();
        loadHandcuffedPlayers();
        setupIpJailFile();
        loadJailedIps();

        translationManager = new TranslationManager(this);

        blockedCommands = getConfig().getStringList("blockedCommands");
        this.handcuffCommand = new Handcuff(this);

        getConfig().addDefault("general.ip-jail-broadcast-message",
                "{prefix} &c{player} has been IP-jailed for {duration} by {jailer}. Reason: {reason}!");
        getConfig().options().copyDefaults(true);
        saveConfig();

        unjailConfirmation = new UnjailConfirmation(this);
        getCommand("unjail").setExecutor(new UnjailCommand(this));
        getCommand("confirmunjail").setExecutor(unjailConfirmation);
        getCommand("cancelunjail").setExecutor(unjailConfirmation);

        DelJailCommand delJailCommand = new DelJailCommand(this);
        getCommand("deljail").setExecutor(delJailCommand);
        getCommand("handledeljail").setExecutor(new HandleDelJailCommand(this, delJailCommand));

        this.handcuffInstance = new Handcuff(this);
        getCommand("handcuff").setExecutor(handcuffInstance);
        getCommand("unhandcuff").setExecutor(new HandcuffRemove(this));

        getCommand("setjail").setExecutor(new JailSetCommand(this));
        getCommand("jail").setExecutor(new JailCommand(this));
        getCommand("jail").setTabCompleter(new JailTabCompleter(this));
        getCommand("deljail").setTabCompleter(new DelJailTabCompleter(this));
        getCommand("jails").setExecutor(new JailsCommand(this));
        getCommand("jailduration").setExecutor(new JailDurationCommand(this));
        getCommand("jailspawn").setExecutor(new JailSpawnCommand(this));
        getCommand("jailspawn").setTabCompleter(new JailSpawnTabCompleter());
        getCommand("jailsreload").setExecutor(new ConfigReload(this));
        getCommand("jailshelp").setExecutor(new JailsHelpCommand());
        getCommand("tempjail").setExecutor(new TempJailCommand(this));
        getCommand("jailsetflag").setExecutor(new SetFlag(this));
        getCommand("jaildelflag").setExecutor(new DelFlag(this));
        getCommand("jailflaglist").setExecutor(new FlagList(this));
        getCommand("jailversion").setExecutor(new JailVersionCommand(this));
        getCommand("ip-jail").setExecutor(new IPJailCommand(this));
        getCommand("ip-jail").setTabCompleter(new IPJailTabCompleter(this));

        JailListCommand jailListCommand = new JailListCommand(this);
        getCommand("jailed").setExecutor(jailListCommand);
        getCommand("jailed").setTabCompleter(jailListCommand);

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new JailListeners(this), this);
        getServer().getPluginManager().registerEvents(new CommandBlocker(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // update checker
        UpdateChecker updateChecker = new UpdateChecker(this);
        if (getConfig().getBoolean("check-updates", true)) {
            updateChecker.fetchRemoteVersion()
                    .thenAccept(remoteVersion -> {
                        String currentVersion = getDescription().getVersion().trim().replace("v", "");
                        if (remoteVersion != null) {
                            String normalizedRemote = remoteVersion.trim().replace("v", "");
                            if (!normalizedRemote.equals(currentVersion)) {
                                logger.info("§6╔════════════════════════════════════╗");
                                logger.info("§6║ §eNew Jails version available!     §6║");
                                logger.info("§6║ §7Current: §c" + currentVersion + " ".repeat(Math.max(0, 20 - currentVersion.length())) + "§6║");
                                logger.info("§6║ §7Latest: §b" + normalizedRemote + " ".repeat(Math.max(0, 20 - normalizedRemote.length())) + "§6║");
                                logger.info("§6╚════════════════════════════════════╝");
                            } else if (getConfig().getBoolean("notify-up-to-date", false)) {
                                logger.info("§aJails is up to date (v" + currentVersion + ")");
                            }
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.warning("Update check failed: " + throwable.getMessage());
                        return null;
                    });
        }

        logger.info("");
        logger.info(JailsChatColor.GOLD + "    +===============+");
        logger.info(JailsChatColor.GOLD + "    |     Jails     |");
        logger.info(JailsChatColor.GOLD + "    |---------------|");
        logger.info(JailsChatColor.GOLD + "    |  Free Version |");
        logger.info(JailsChatColor.GOLD + "    +===============+");
        logger.info("");
        logger.info(JailsChatColor.AQUA + "    Purchase Jails+ for more features!");
        logger.info(JailsChatColor.GREEN + "    " + getPurchaseLink());
        logger.info("");
    }

    @Override
    public void onDisable() {
        saveJailedPlayersConfig();
        saveJailLocationsConfig();
        saveHandcuffedPlayersConfig();
        saveJailedIps();
        logger.info("Jails has been disabled!");
    }

    public void loadConfigValues() {
        this.prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix", "&7[&eJails&7]"));
        this.alertMessages = getConfig().getBoolean("jail-settings.enable-escape-alerts", true);
        this.targetSkin = getConfig().getString("jail-settings.skin-username", "SirMothsho");
        this.handcuffSpeed = getConfig().getDouble("handcuff-settings.handcuff-speed", 0.05);
    }

    public void reloadPluginConfig(){
        reloadConfig();
        handcuffInstance.reloadSettings();
        loadConfigValues();
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
            logger.info("Loaded jail: " + key);
        }
    }

    public FileConfiguration getJailLocationsConfig() {
        return jailLocationsConfig;
    }

    public void saveJailLocationsConfig() {
        if (jailLocationsConfig != null && jailLocationsFile != null) {
            try {
                jailLocationsConfig.save(jailLocationsFile);
            } catch (IOException e) {
                getLogger().severe("Could not save jail_locations.yml!");
                e.printStackTrace();
            }
        } else {
            getLogger().warning("Skipped saving jail_locations.yml because config or file is null.");
        }
    }

    // --- Handcuffed Players ---
    public void handcuffPlayer(Player player) {
        UUID playerUUID = player.getUniqueId();
        String basePath = playerUUID.toString();
        if(getConfig().getBoolean("handcuff-settings.slow-movement")){
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(handcuffSpeed);
        }
        if(getConfig().getBoolean("handcuff-settings.blindness")){
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 1, true, false));
        }

        handcuffedPlayersConfig.set(basePath + ".handcuffed", true);
        saveHandcuffedPlayersConfig();


        logger.info("Player " + player.getName() + " has been handcuffed.");
    }

    public void unHandcuffPlayer(UUID playerUUID) {
        if (!isPlayerHandcuffed(playerUUID)) {
            logger.warning("Player with UUID " + playerUUID + " is not handcuffed.");
            return;
        }

        Player target = Bukkit.getPlayer(playerUUID);

        target.removePotionEffect(PotionEffectType.DARKNESS);
        target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);

        handcuffedPlayersConfig.set(playerUUID.toString(), null);
        saveHandcuffedPlayersConfig();

        logger.info("Player with UUID " + playerUUID + " has been unhandcuffed.");
    }


    public FileConfiguration getHandcuffedPlayersConfig() {
        return handcuffedPlayersConfig;
    }

    public void saveHandcuffedPlayersConfig() {
        if (handcuffedPlayersConfig != null && handcuffedPlayersFile != null) {
            try {
                handcuffedPlayersConfig.save(handcuffedPlayersFile);
            } catch (IOException e) {
                getLogger().severe("Could not save handcuffed_players.yml");
                e.printStackTrace();
            }
        } else {
            getLogger().warning("Skipped saving handcuffed_players.yml because config or file is null.");
        }
    }

    public boolean isPlayerHandcuffed(UUID playerUUID) {
        return handcuffedPlayersConfig.contains(playerUUID.toString());
    }

    public void loadHandcuffedPlayers() {
        for (String key : handcuffedPlayersConfig.getKeys(false)) {
            logger.info("Loaded jailed player: " + key);
        }
    }

    // --- Jailed Players ---

    public void jailPlayer(Player player, String jailName, long endTime, String reason, String jailer) {
        Location jailLocation = getJail(jailName);


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

            case null:
                setPlayerSpawnOption(playerUUID, "world_spawn");
                break;
            default:
                setPlayerSpawnOption(playerUUID, "world_spawn");
                break;
        }
        saveJailedPlayersConfig();



        if (jailLocation != null) {
            player.teleport(jailLocation);
        } else {
            logger.warning("Jail location for " + jailName + " not found.");
        }
        Bukkit.getPluginManager().callEvent(new PlayerJailEvent(player));
    }


    public void unjailPlayer(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        String spawnOption = getPlayerSpawnOption(playerUUID);

        if (player == null) {
            jailedPlayersConfig.set(playerUUID.toString() + ".unjailed", true);
            saveJailedPlayersConfig();
            logger.info("Offline player has been marked as unjailed.");
            return;
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
        logger.info("Player " + player.getName() + " has been unjailed and their data removed.");
    }

    public void teleportToOriginalLocation(Player player, String basePath) {
        String worldName = jailedPlayersConfig.getString(basePath + ".world");
        if (worldName == null || Bukkit.getWorld(worldName) == null) {
            logger.warning("Could not teleport player " + player.getName() + " back to their original location. World '" + worldName + "' not found.");
            return;
        }

        double x = jailedPlayersConfig.getDouble(basePath + ".x");
        double y = jailedPlayersConfig.getDouble(basePath + ".y");
        double z = jailedPlayersConfig.getDouble(basePath + ".z");
        float yaw = (float) jailedPlayersConfig.getDouble(basePath + ".yaw");
        float pitch = (float) jailedPlayersConfig.getDouble(basePath + ".pitch");

        Location originalLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
        player.teleport(originalLocation);
        logger.info("Teleported player " + player.getName() + " back to their original location.");
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
            logger.info("Loaded jailed player: " + key);
        }
    }

    public Map<String, JailedIpInfo> getJailedIps() {
        return jailedIps;
    }

    public FileConfiguration getJailedPlayersConfig() {
        return jailedPlayersConfig;
    }

    public void saveJailedPlayersConfig() {
        try {
            jailedPlayersConfig.save(jailedPlayersFile);
        } catch (IOException e) {
            logger.severe("Could not save jailed_players.yml!");
        }
    }

    // --- ip-jail ---

    public class JailedIpInfo {
        private final String jailName;
        private final long releaseTime;
        private final String reason;
        private final String jailer;

        public JailedIpInfo(String jailName, long releaseTime, String reason, String jailer) {
            this.jailName = jailName;
            this.releaseTime = releaseTime;
            this.reason = reason;
            this.jailer = jailer;
        }

        public String getJailName() {
            return jailName;
        }

        public long getReleaseTime() {
            return releaseTime;
        }

        public String getReason() {
            return reason;
        }

        public String getJailer() {
            return jailer;
        }

        public boolean isExpired() {
            return releaseTime > 0 && System.currentTimeMillis() > releaseTime;
        }
    }

    private void setupIpJailFile() {
        ipJailFile = new File(getDataFolder(), "ip-jail-data.yml");

        if (!ipJailFile.exists()) {
            try {
                ipJailFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create ip-jail-data.yml file: " + e.getMessage());
            }
        }

        ipJailConfig = YamlConfiguration.loadConfiguration(ipJailFile);

        if (!ipJailConfig.contains("security.ip-salt")) {
            ipJailConfig.set("security.ip-salt", generateRandomSalt());
            saveIpJailConfig();
        }
    }

    private void saveIpJailConfig() {
        try {
            ipJailConfig.save(ipJailFile);
        } catch (IOException e) {
            getLogger().severe("Could not save ip-jail-data.yml file: " + e.getMessage());
        }
    }

    public String hashIpAddress(String ip) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedIp = ip + ipJailConfig.getString("security.ip-salt", "default-salt");
            byte[] hash = digest.digest(saltedIp.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            getLogger().severe("Failed to hash IP address: " + e.getMessage());
            return null;
        }
    }

    public void addJailedIp(String hashedIp, String jailName, long duration, String reason, String jailer) {
        long releaseTime = duration > 0 ? System.currentTimeMillis() + duration : -1;
        jailedIps.put(hashedIp, new JailedIpInfo(jailName, releaseTime, reason, jailer));
        saveJailedIps();
    }

    public void removeJailedIp(String hashedIp) {
        jailedIps.remove(hashedIp);
        saveJailedIps();
    }

    public boolean removePlayerIpJail(Player player) {
        String playerIp = player.getAddress().getAddress().getHostAddress();
        String hashedIp = hashIpAddress(playerIp);

        if (isIpJailed(hashedIp)) {
            removeJailedIp(hashedIp);
            return true;
        }
        return false;
    }

    public String getPlayerHashedIp(Player player) {
        String playerIp = player.getAddress().getAddress().getHostAddress();
        return hashIpAddress(playerIp);
    }

    public boolean isIpJailed(String hashedIp) {
        if (!jailedIps.containsKey(hashedIp)) {
            return false;
        }

        JailedIpInfo info = jailedIps.get(hashedIp);
        if (info.isExpired()) {
            removeJailedIp(hashedIp);
            return false;
        }

        return true;
    }

    public JailedIpInfo getJailedIpInfo(String hashedIp) {
        if (!isIpJailed(hashedIp)) {
            return null;
        }
        return jailedIps.get(hashedIp);
    }

    public void saveJailedIps() {
        if (ipJailConfig == null) {
            getLogger().warning("Cannot save jailed IPs: ipJailConfig is null.");
            return;
        }

        jailedIps.entrySet().removeIf(entry -> entry.getValue().isExpired());

        ipJailConfig.set("jailed-ips", null);

        for (Map.Entry<String, JailedIpInfo> entry : jailedIps.entrySet()) {
            String path = "jailed-ips." + entry.getKey();
            JailedIpInfo info = entry.getValue();

            ipJailConfig.set(path + ".jail-name", info.getJailName());
            ipJailConfig.set(path + ".release-time", info.getReleaseTime());
            ipJailConfig.set(path + ".reason", info.getReason());
            ipJailConfig.set(path + ".jailer", info.getJailer());
        }

        saveIpJailConfig();
    }

    public void loadJailedIps() {
        jailedIps.clear();

        if (ipJailConfig.getConfigurationSection("jailed-ips") == null) {
            return;
        }

        for (String hashedIp : ipJailConfig.getConfigurationSection("jailed-ips").getKeys(false)) {
            String path = "jailed-ips." + hashedIp;

            String jailName = ipJailConfig.getString(path + ".jail-name");
            long releaseTime = ipJailConfig.getLong(path + ".release-time");
            String reason = ipJailConfig.getString(path + ".reason");
            String jailer = ipJailConfig.getString(path + ".jailer");

            if (releaseTime > 0 && System.currentTimeMillis() > releaseTime) {
                continue;
            }

            jailedIps.put(hashedIp, new JailedIpInfo(jailName, releaseTime, reason, jailer));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerIp = player.getAddress().getAddress().getHostAddress();
        String hashedIp = hashIpAddress(playerIp);

        if (isIpJailed(hashedIp) && !isPlayerJailed(player.getUniqueId())) {
            JailedIpInfo info = getJailedIpInfo(hashedIp);
            long remainingTime = info.getReleaseTime() > 0 ? info.getReleaseTime() - System.currentTimeMillis() : -1;

            jailPlayer(player, info.getJailName(), remainingTime,
                    "IP associated with a jailed account. Original reason: " + info.getReason(),
                    "SYSTEM (IP-Jail)");

            player.sendMessage(getPrefix() + " " +
                    "You have been automatically jailed because your IP address is associated with a jailed account.");
        }
    }

    private String generateRandomSalt() {
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < 16; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
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
                logger.severe("Could not create handcuffed_players.yml!");
            }
        }
        handcuffedPlayersConfig = YamlConfiguration.loadConfiguration(handcuffedPlayersFile);


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
        if (duration <= 0) {
            return;
        }
        String prefix = getPrefix();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                unjailPlayer(player.getUniqueId());
                String messageTemplate = getConfig().getString("general.unjail-broadcast-message",
                        "{prefix} &c{player} has been unjailed.");
                String broadcastMessage = messageTemplate
                        .replace("{prefix}", ChatColor.translateAlternateColorCodes('&', prefix))
                        .replace("{player}", player.getName());
                if (getConfig().getBoolean("general.broadcast-on-unjail")) {
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
                }
            } else if (isPlayerJailed(player.getUniqueId())) {
                unjailPlayer(player.getUniqueId());
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

    public void sendJailsPlusMessage(Player sender){
        String message1 = JailsChatColor.GOLD + translationManager.getMessage("jailsplus_access");
        sender.sendMessage(JailsChatColor.BOLD + message1);
        TextComponent message = new TextComponent (translationManager.getMessage("jailsplus_access"));
        message.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        message.setBold(true);
        message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, getPurchaseLink()));
        sender.spigot().sendMessage(message);
    }

    public TranslationManager getTranslationManager() {
        return translationManager;
    }

    public String getPurchaseLink(){
        return purchaseLink;
    }
}