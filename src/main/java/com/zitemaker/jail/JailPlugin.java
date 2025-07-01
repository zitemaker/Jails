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


package com.zitemaker.jail;

import com.zitemaker.jail.commands.*;
import com.zitemaker.jail.confirmations.HandleDelJailCommand;
import com.zitemaker.jail.confirmations.UnjailConfirmation;
import com.zitemaker.jail.listeners.ChatListener;
import com.zitemaker.jail.listeners.CommandBlocker;
import com.zitemaker.jail.listeners.JailListeners;
import com.zitemaker.jail.listeners.PlayerJailEvent;
import com.zitemaker.jail.translation.LangNotice;
import com.zitemaker.jail.update.JailVersionCommand;
import com.zitemaker.jail.update.UpdateChecker;
import com.zitemaker.jail.utils.*;
import com.zitemaker.jail.translation.TranslationManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
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

public class JailPlugin extends JavaPlugin {
    private File jailedPlayersFile;
    private FileConfiguration jailedPlayersConfig;

    private File jailLocationsFile;
    private FileConfiguration jailLocationsConfig;
    public List<String> blockedCommands;
    private File handcuffedPlayersFile;
    private FileConfiguration handcuffedPlayersConfig;

    public String prefix;
    public boolean alertMessages;
    public String targetSkin;
    public double handcuffSpeed;
    public String purchaseLink = "https://builtbybit.com/resources/jails.62499/";
    public UnjailConfirmation unjailConfirmation;
    private final Console console = new SpigotConsole();
    private final Logger logger = new Logger(new JavaPlatformLogger(console, getLogger()), true);
    private TranslationManager translationManager;
    private Handcuff handcuffInstance;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        createFiles();
        loadJails();
        loadJailedPlayers();
        loadHandcuffedPlayers();
        LangNotice.create(this);

        translationManager = new TranslationManager(this);
        saveConfig();

        unjailConfirmation = new UnjailConfirmation(this);

        registerCommand("unjail", new UnjailCommand(this));
        registerCommand("confirmunjail", unjailConfirmation);
        registerCommand("cancelunjail", unjailConfirmation);

        DelJailCommand delJailCommand = new DelJailCommand(this);
        registerCommand("deljail", delJailCommand);
        registerCommand("handledeljail", new HandleDelJailCommand(this, delJailCommand));

        this.handcuffInstance = new Handcuff(this);
        registerCommand("handcuff", handcuffInstance);
        registerCommand("unhandcuff", new HandcuffRemove(this));

        registerCommand("setjail", new JailSetCommand(this));

        PluginCommand jailCmd = getCommand("jail");
        if (jailCmd != null) {
            jailCmd.setExecutor(new JailCommand(this));
            jailCmd.setTabCompleter(new JailTabCompleter(this));
        } else {
            getLogger().warning("Command 'jail' not found in plugin.yml");
        }

        PluginCommand delJailCmd = getCommand("deljail");
        if (delJailCmd != null) {
            delJailCmd.setTabCompleter(new DelJailTabCompleter(this));
        }

        registerCommand("jails", new JailsCommand(this));
        registerCommand("jailduration", new JailDurationCommand(this));

        PluginCommand jailSpawnCmd = getCommand("jailspawn");
        if (jailSpawnCmd != null) {
            jailSpawnCmd.setExecutor(new JailSpawnCommand(this));
            jailSpawnCmd.setTabCompleter(new JailSpawnTabCompleter());
        }

        PluginCommand TempJailCmd = getCommand("tempjail");
        if (TempJailCmd !=null) {
            TempJailCmd.setExecutor(new TempJailCommand(this));
            TempJailCmd.setTabCompleter(new TempJailTabCompleter(this));
        }

        registerCommand("jailsreload", new ConfigReload(this));
        registerCommand("jailshelp", new JailsHelpCommand());
        registerCommand("jailversion", new JailVersionCommand(this));

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new JailListeners(this), this);
        getServer().getPluginManager().registerEvents(new CommandBlocker(this), this);

        // Bstats
        new Metrics(this, 25128);

        // Update checker
        UpdateChecker updateChecker = new UpdateChecker(this);
        if (getConfig().getBoolean("check-updates", true)) {
            updateChecker.fetchRemoteVersion()
                    .thenAccept(remoteVersion -> {
                        String currentVersion = getDescription().getVersion().trim().replace("v", "");
                        if (remoteVersion != null) {
                            String normalizedRemote = remoteVersion.trim().replace("v", "");
                            if (!normalizedRemote.equals(currentVersion)) {
                                int boxWidth = 35;
                                int currentLineLength = "Current: ".length() + currentVersion.length();
                                int remoteLineLength = "Latest: ".length() + normalizedRemote.length();

                                int currentPadding = boxWidth - currentLineLength;
                                int remotePadding = boxWidth - remoteLineLength;

                                logger.info("§6+====================================+");
                                logger.info("§6| §eNew Jails version available!       §6|");
                                logger.info("§6| §7Current: §c" + currentVersion + " ".repeat(Math.max(0, currentPadding)) + "§6|");
                                logger.info("§6| §7Latest: §b" + normalizedRemote + " ".repeat(Math.max(0, remotePadding)) + "§6|");
                                logger.info("§6+====================================+");
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

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
        } else {
            getLogger().warning("Command not found in plugin.yml: " + name);
        }
    }

    @Override
    public void onDisable() {
        saveJailedPlayersConfig();
        saveJailLocationsConfig();
        saveHandcuffedPlayersConfig();
        logger.info("Jails has been disabled!");
    }

    public void loadConfigValues() {
        this.prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix", "&7[&eJails&7]"));
        this.alertMessages = getConfig().getBoolean("jail-settings.enable-escape-alerts", true);
        this.targetSkin = getConfig().getString("jail-settings.skin-username", "SirMothsho");
        this.handcuffSpeed = getConfig().getDouble("handcuff-settings.handcuff-speed", 0.05);
        this.blockedCommands = getConfig().getStringList("blocked-commands");
    }

    public void reloadPluginConfig(){
        reloadConfig();
        handcuffInstance.reloadSettings();
        translationManager.reloadMessages();
        loadConfigValues();
    }


    // ------ Jail Locations ------
    public void addJail(String name, Location location) {
        jailLocationsConfig.set(name + ".world", Objects.requireNonNull(location.getWorld()).getName());
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

        return new Location(Bukkit.getWorld(Objects.requireNonNull(worldName)), x, y, z, yaw, pitch);
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

    public void saveJailLocationsConfig() {
        if (jailLocationsConfig != null && jailLocationsFile != null) {
            try {
                jailLocationsConfig.save(jailLocationsFile);
            } catch (IOException e) {
                getLogger().severe("Could not save jail_locations.yml!");
                for (StackTraceElement element : e.getStackTrace()) {
                    getLogger().warning(element.toString());
                }
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
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(handcuffSpeed);
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

        Objects.requireNonNull(target).removePotionEffect(PotionEffectType.DARKNESS);
        Objects.requireNonNull(target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.1);

        handcuffedPlayersConfig.set(playerUUID.toString(), null);
        saveHandcuffedPlayersConfig();

        logger.info("Player with UUID " + playerUUID + " has been unhandcuffed.");
    }

    public void saveHandcuffedPlayersConfig() {
        if (handcuffedPlayersConfig != null && handcuffedPlayersFile != null) {
            try {
                handcuffedPlayersConfig.save(handcuffedPlayersFile);
            } catch (IOException e) {
                getLogger().severe("Could not save handcuffed_players.yml");
                for (StackTraceElement element : e.getStackTrace()) {
                    getLogger().warning(element.toString());
                }
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
        jailedPlayersConfig.set(basePath + ".original.world", Objects.requireNonNull(originalLocation.getWorld()).getName());
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
            jailedPlayersConfig.set(playerUUID + ".unjailed", true);
            saveJailedPlayersConfig();
            logger.info("Offline player has been marked as unjailed.");
            return;
        }


        if ("world_spawn".equals(spawnOption)) {
            Location worldSpawn = player.getWorld().getSpawnLocation();
            player.teleport(worldSpawn);
        } else if ("original_location".equals(spawnOption)) {
            teleportToOriginalLocation(player, playerUUID + ".original");
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

    public void loadJailedPlayers() {
        for (String key : jailedPlayersConfig.getKeys(false)) {
            logger.info("Loaded jailed player: " + key);
        }
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
                boolean ignoreReturn = handcuffedPlayersFile.createNewFile();
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
                String messageTemplate = translationManager.getMessage("unjail_broadcast");
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

    public void sendJailsPlusMessage(Player sender) {
        String language = getConfig().getString("language", "en").toLowerCase();
        String accessMessage;
        String purchaseMessage = switch (language) {
            case "de" -> {
                accessMessage = "Du versuchst, eine Funktion zu nutzen, die nur in Jails+ verfügbar ist.";
                yield "Klicke hier, um Jails+ zu kaufen!";
            }
            case "es" -> {
                accessMessage = "Estás intentando usar una función que solo está disponible en Jails+.";
                yield "¡Haz clic aquí para comprar Jails+!";
            }
            case "fr" -> {
                accessMessage = "Vous essayez d'utiliser une fonctionnalité disponible uniquement dans Jails+.";
                yield "Cliquez ici pour acheter Jails+ !";
            }
            case "ru" -> {
                accessMessage = "Вы пытаетесь использовать функцию, доступную только в Jails+.";
                yield "Нажмите здесь, чтобы купить Jails+!";
            }
            default -> {
                accessMessage = "You are trying to use a feature that is only available in Jails+.";
                yield "Click here to purchase Jails+!";
            }
        };
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + accessMessage);
        TextComponent message = new TextComponent(purchaseMessage);
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