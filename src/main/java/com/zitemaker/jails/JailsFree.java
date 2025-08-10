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

package com.zitemaker.jails;

import com.zitemaker.jails.commands.*;
import com.zitemaker.jails.commands.subcommands.*;
import com.zitemaker.jails.commands.tabcompleter.*;
import com.zitemaker.jails.commands.confirmations.*;
import com.zitemaker.jails.commands.listeners.*;
import com.zitemaker.jails.translation.*;
import com.zitemaker.jails.update.*;
import com.zitemaker.jails.utils.*;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.Listener;

public class JailsFree extends JavaPlugin {
    private static final String JAILED_PLAYERS_FILE = "jailed_players.yml";
    private static final String JAIL_LOCATIONS_FILE = "jail_locations.yml";
    private static final String HANDCUFFED_PLAYERS_FILE = "handcuffed_players.yml";
    private static final int BSTATS_ID = 25128;

    private File jailedPlayersFile;
    @Getter private FileConfiguration jailedPlayersConfig;
    private File jailLocationsFile;
    private FileConfiguration jailLocationsConfig;
    private File handcuffedPlayersFile;
    private FileConfiguration handcuffedPlayersConfig;
    private JailListeners jailListeners;

    @Getter private String prefix;
    private double handcuffSpeed;
    @Getter private final String purchaseLink = "https://builtbybit.com/resources/jails.62499/";
    public List<String> blockedCommands;

    private final Console console = new SpigotConsole();
    private final Logger logger = new Logger(new JavaPlatformLogger(console, getLogger()), true);
    @Getter private TranslationManager translationManager;
    private HandcuffSC handcuffSCInstance;
    @Getter
    private UnjailCF unjailCF;

    @Getter
    private String lang;

    @Override
    public void onEnable() {
        initializePlugin();
        registerCommandsAndListeners();
        checkForUpdates();
        logStartup();
    }

    private void initializePlugin() {
        saveDefaultConfig();
        loadConfigValues();
        createFiles();
        loadJails();
        loadJailedPlayers();
        loadHandcuffedPlayers();
        LangNotice.create(this);

        handcuffSCInstance = new HandcuffSC(this);
        translationManager = new TranslationManager(this);
        saveConfig();
    }

    private void registerCommandsAndListeners() {
        RootCommand rootCommand = new RootCommand(this);
        registerCommand("jails", rootCommand, new TabCompleter(rootCommand, this));

        unjailCF = new UnjailCF(this);
        registerCommand("confirmunjail", unjailCF);
        registerCommand("cancelunjail", unjailCF);

        DeleteJailSC deleteJailSC = new DeleteJailSC(this);
        registerCommand("deljailcf", new DelJailCF(this, deleteJailSC));

        registerEvent(new ChatListener(this));
        jailListeners = new JailListeners(this);
        registerEvent(jailListeners);
        registerEvent(new CommandBlocker(this));

        new Metrics(this, BSTATS_ID);
    }

    private void registerCommand(String name, CommandExecutor executor) {
        registerCommand(name, executor, null);
    }

    private void registerCommand(String name, CommandExecutor executor, org.bukkit.command.TabCompleter tabCompleter) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            if (tabCompleter != null) cmd.setTabCompleter(tabCompleter);
        } else {
            logger.warning("Command not found: " + name);
        }
    }

    private void registerEvent(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private void checkForUpdates() {
        if (!getConfig().getBoolean("check-updates", true)) return;

        new UpdateChecker(this).fetchRemoteVersion().thenAccept(remoteVersion -> {
            String currentVersion = getDescription().getVersion().trim().replace("v", "");
            if (remoteVersion == null) return;

            String normalizedRemote = remoteVersion.trim().replace("v", "");
            if (normalizedRemote.equals(currentVersion)) {
                if (getConfig().getBoolean("notify-up-to-date", false)) {
                    logger.info("§aJails is up to date (v" + currentVersion + ")");
                }
                return;
            }

            int boxWidth = 35;
            int currentPadding = boxWidth - ("Current: ".length() + currentVersion.length());
            int remotePadding = boxWidth - ("Latest: ".length() + normalizedRemote.length());

            logger.info("§6+====================================+");
            logger.info("§6| §eNew Jails version available!       §6|");
            logger.info("§6| §7Current: §c" + currentVersion + " ".repeat(Math.max(0, currentPadding)) + "§6|");
            logger.info("§6| §7Latest: §b" + normalizedRemote + " ".repeat(Math.max(0, remotePadding)) + "§6|");
            logger.info("§6+====================================+");
        }).exceptionally(throwable -> {
            logger.warning("Update check failed: " + throwable.getMessage());
            return null;
        });
    }

    private void logStartup() {
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
        saveConfigFile(jailedPlayersConfig, jailedPlayersFile);
        saveConfigFile(jailLocationsConfig, jailLocationsFile);
        saveConfigFile(handcuffedPlayersConfig, handcuffedPlayersFile);
        logger.info("Jails has been disabled!");
    }

    public void loadConfigValues() {
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix", "&7[&eJails&7]"));
        handcuffSpeed = getConfig().getDouble("handcuff-settings.handcuff-speed", 0.05);
        blockedCommands = getConfig().getStringList("blocked-commands");
        lang = getConfig().getString("language", "en").toLowerCase();
    }

    public void reloadPluginConfig() {
        reloadConfig();
        handcuffSCInstance.reloadSettings();
        translationManager.reloadMessages();
        loadConfigValues();
    }

    public void cleanupPlayerData(UUID playerUUID) {
        if (jailListeners != null) {
            jailListeners.cleanupPlayerCooldowns(playerUUID);
        }
    }

    // ============== Jails Location Management ================
    public void addJail(String name, Location location) {
        World world = location.getWorld();
        jailLocationsConfig.set(name + ".world", world != null ? world.getName() : null);
        jailLocationsConfig.set(name + ".x", location.getX());
        jailLocationsConfig.set(name + ".y", location.getY());
        jailLocationsConfig.set(name + ".z", location.getZ());
        jailLocationsConfig.set(name + ".yaw", location.getYaw());
        jailLocationsConfig.set(name + ".pitch", location.getPitch());
        saveConfigFile(jailLocationsConfig, jailLocationsFile);
    }

    public Location getJail(String name) {
        if (!jailLocationsConfig.contains(name)) return null;

        World world = Bukkit.getWorld(Objects.requireNonNull(jailLocationsConfig.getString(name + ".world")));
        if (world == null) return null;

        return new Location(
                world,
                jailLocationsConfig.getDouble(name + ".x"),
                jailLocationsConfig.getDouble(name + ".y"),
                jailLocationsConfig.getDouble(name + ".z"),
                (float) jailLocationsConfig.getDouble(name + ".yaw"),
                (float) jailLocationsConfig.getDouble(name + ".pitch")
        );
    }

    public Map<String, Location> getJails() {
        Map<String, Location> jails = new HashMap<>();
        jailLocationsConfig.getKeys(false).forEach(key -> {
            Location location = getJail(key);
            if (location != null) jails.put(key, location);
        });
        return jails;
    }

    public void removeJail(String name) {
        jailLocationsConfig.set(name, null);
        saveConfigFile(jailLocationsConfig, jailLocationsFile);
    }

    public void loadJails() {
        jailLocationsConfig.getKeys(false).forEach(key -> logger.info("Loaded jail: " + key));
    }

    // ==================== Handcuff ===============================
    public void handcuffPlayer(Player player) {
        UUID playerUUID = player.getUniqueId();
        String basePath = playerUUID.toString();

        if (getConfig().getBoolean("handcuff-settings.slow-movement")) {
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(handcuffSpeed);
        }
        if (getConfig().getBoolean("handcuff-settings.blindness")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 1, true, false));
        }

        handcuffedPlayersConfig.set(basePath + ".handcuffed", true);
        saveConfigFile(handcuffedPlayersConfig, handcuffedPlayersFile);
        logger.info("Handcuffed player: " + player.getName());
    }

    public void unHandcuffPlayer(UUID playerUUID) {
        if (!isPlayerHandcuffed(playerUUID)) {
            logger.warning("Player not handcuffed: " + playerUUID);
            return;
        }

        Player target = Bukkit.getPlayer(playerUUID);
        if (target != null) {
            target.removePotionEffect(PotionEffectType.DARKNESS);
            Objects.requireNonNull(target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.1);
        }

        handcuffedPlayersConfig.set(playerUUID.toString(), null);
        saveConfigFile(handcuffedPlayersConfig, handcuffedPlayersFile);
        logger.info("Unhandcuffed player: " + playerUUID);
    }

    public boolean isPlayerHandcuffed(UUID playerUUID) {
        return handcuffedPlayersConfig.contains(playerUUID.toString());
    }

    public void loadHandcuffedPlayers() {
        handcuffedPlayersConfig.getKeys(false).forEach(key -> logger.info("Loaded handcuffed player: " + key));
    }

    // ===================== Jail Management ==========================
    public void jailPlayer(Player player, String jailName, long endTime, String reason, String jailer) {
        UUID playerUUID = player.getUniqueId();
        String basePath = playerUUID.toString();
        Location originalLocation = player.getLocation();
        World world = originalLocation.getWorld();

        jailedPlayersConfig.set(basePath + ".original.world", world != null ? world.getName() : null);
        jailedPlayersConfig.set(basePath + ".original.x", originalLocation.getX());
        jailedPlayersConfig.set(basePath + ".original.y", originalLocation.getY());
        jailedPlayersConfig.set(basePath + ".original.z", originalLocation.getZ());
        jailedPlayersConfig.set(basePath + ".original.yaw", originalLocation.getYaw());
        jailedPlayersConfig.set(basePath + ".original.pitch", originalLocation.getPitch());

        jailedPlayersConfig.set(basePath + ".jailName", jailName);
        jailedPlayersConfig.set(basePath + ".endTime", endTime);
        jailedPlayersConfig.set(basePath + ".reason", reason);
        jailedPlayersConfig.set(basePath + ".jailer", jailer);

        String spawnOption = getConfig().getString("general.default-unjail-location", "WORLD_SPAWN");
        setPlayerSpawnOption(playerUUID, spawnOption.equalsIgnoreCase("ORIGINAL_LOCATION") ?
                "original_location" : "world_spawn");

        saveConfigFile(jailedPlayersConfig, jailedPlayersFile);

        Location jailLocation = getJail(jailName);
        if (jailLocation != null) {
            player.teleport(jailLocation);
        } else {
            logger.warning("Jail location not found: " + jailName);
        }
        Bukkit.getPluginManager().callEvent(new PlayerJailEvent(player));
    }

    public void unjailPlayer(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            jailedPlayersConfig.set(playerUUID + ".unjailed", true);
            saveConfigFile(jailedPlayersConfig, jailedPlayersFile);
            logger.info("Marked offline player as unjailed: " + playerUUID);
            cleanupPlayerData(playerUUID);
            return;
        }

        String spawnOption = getPlayerSpawnOption(playerUUID);
        if ("world_spawn".equals(spawnOption)) {
            player.teleport(player.getWorld().getSpawnLocation());
        } else if ("original_location".equals(spawnOption)) {
            teleportToOriginalLocation(player, playerUUID + ".original");
        } else {
            player.teleport(player.getWorld().getSpawnLocation());
        }

        jailedPlayersConfig.set(playerUUID.toString(), null);
        saveConfigFile(jailedPlayersConfig, jailedPlayersFile);

        cleanupPlayerData(playerUUID);

        logger.info("Unjailed player: " + player.getName());
    }

    private void teleportToOriginalLocation(Player player, String basePath) {
        String worldName = jailedPlayersConfig.getString(basePath + ".world");
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;

        if (world == null) {
            logger.warning("World not found for player: " + player.getName());
            return;
        }

        Location originalLocation = new Location(
                world,
                jailedPlayersConfig.getDouble(basePath + ".x"),
                jailedPlayersConfig.getDouble(basePath + ".y"),
                jailedPlayersConfig.getDouble(basePath + ".z"),
                (float) jailedPlayersConfig.getDouble(basePath + ".yaw"),
                (float) jailedPlayersConfig.getDouble(basePath + ".pitch")
        );
        player.teleport(originalLocation);
        logger.info("Restored original location for: " + player.getName());
    }

    public void setPlayerSpawnOption(UUID playerUUID, String option) {
        jailedPlayersConfig.set(playerUUID.toString() + ".spawnOption", option);
        saveConfigFile(jailedPlayersConfig, jailedPlayersFile);
    }

    public String getPlayerSpawnOption(UUID playerUUID) {
        return jailedPlayersConfig.getString(playerUUID.toString() + ".spawnOption", "original_location");
    }

    public boolean isPlayerJailed(UUID playerUUID) {
        return jailedPlayersConfig.contains(playerUUID.toString());
    }

    public void loadJailedPlayers() {
        jailedPlayersConfig.getKeys(false).forEach(key -> logger.info("Loaded jailed player: " + key));
    }

    // ================== File Management ==================
    private void createFiles() {
        jailedPlayersFile = setupConfigFile(JAILED_PLAYERS_FILE, true);
        jailedPlayersConfig = YamlConfiguration.loadConfiguration(jailedPlayersFile);

        jailLocationsFile = setupConfigFile(JAIL_LOCATIONS_FILE, true);
        jailLocationsConfig = YamlConfiguration.loadConfiguration(jailLocationsFile);

        handcuffedPlayersFile = setupConfigFile(HANDCUFFED_PLAYERS_FILE, false);
        handcuffedPlayersConfig = YamlConfiguration.loadConfiguration(handcuffedPlayersFile);
    }

    private File setupConfigFile(String fileName, boolean fromResource) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            if (fromResource) {
                saveResource(fileName, false);
            } else {
                try {
                    if (!file.createNewFile()) {
                        logger.severe("Could not create " + fileName + "!");
                    }
                } catch (IOException e) {
                    logger.severe("Could not create " + fileName + "!");
                }
            }
        }
        return file;
    }

    private void saveConfigFile(FileConfiguration config, File file) {
        if (config == null || file == null) {
            logger.warning("Skipped saving file: config/file is null");
            return;
        }
        try {
            config.save(file);
        } catch (IOException e) {
            logger.severe("Could not save file: " + file.getName());
        }
    }

    // ===================== Utilities ========================
    public static long parseDuration(String input) {
        long time = 0L;
        StringBuilder number = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else if ("smhd".indexOf(c) >= 0 && !number.isEmpty()) {
                int value = Integer.parseInt(number.toString());
                switch (c) {
                    case 's': time += value * 1000L; break;
                    case 'm': time += value * 60 * 1000L; break;
                    case 'h': time += value * 60 * 60 * 1000L; break;
                    case 'd': time += value * 24 * 60 * 60 * 1000L; break;
                }
                number.setLength(0);
            }
        }
        return time;
    }

    public void scheduleUnjail(Player player, long duration) {
        if (duration <= 0) return;

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) {
                if (isPlayerJailed(player.getUniqueId())) unjailPlayer(player.getUniqueId());
                return;
            }

            unjailPlayer(player.getUniqueId());
            if (!getConfig().getBoolean("general.broadcast-on-unjail")) return;

            String message = translationManager.getMessage("unjail_broadcast")
                    .replace("{prefix}", prefix)
                    .replace("{player}", player.getName());
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
        }, duration / 50);
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

    public List<String> getJailedPlayerNames() {
        List<String> names = new ArrayList<>();
        for (String uuidStr : jailedPlayersConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
                    names.add(offlinePlayer.getName());
                }
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            }
        }
        return names;
    }

    public List<String> getHandcuffedPlayerNames() {
        List<String> names = new ArrayList<>();
        for (String uuidStr : handcuffedPlayersConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
                    names.add(offlinePlayer.getName());
                }
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            }
        }
        return names;
    }

    public JailListeners getJailListeners() {
        return jailListeners;
    }
}