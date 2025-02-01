package me.zitemaker.jail;

import me.zitemaker.jail.commands.*;
import me.zitemaker.jail.listeners.*;
import me.zitemaker.jail.utils.*;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.ConsoleCommandSender;
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
import java.util.logging.Level;

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

    private final Map<UUID, String> playerSpawnPreferences = new HashMap<>();
    private final Set<UUID> alreadyAlerted = new HashSet<>();
    private final Set<String> notifiedInsecureJails = new HashSet<>();
    public String prefix;
    public boolean alertMessages;
    public String targetSkin;
    public double handcuffSpeed;
    public String purchaseLink = "https://zitemaker.tebex.io";
    private Console console = new SpigotConsole();;
    private PlatformLogger platformLogger;
    private Logger logger = new Logger(new JavaPlatformLogger(console, getLogger()), true);
    private final boolean loggerColor = true;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        createFiles();
        loadJails();
        loadJailedPlayers();
        loadHandcuffedPlayers();

        blockedCommands = getConfig().getStringList("blockedCommands");

        getCommand("setjail").setExecutor(new JailSetCommand(this));
        getCommand("jail").setExecutor(new JailCommand(this));
        getCommand("jail").setTabCompleter(new JailTabCompleter(this));
        getCommand("deljail").setExecutor(new DelJailCommand(this));
        getCommand("deljail").setTabCompleter(new DelJailTabCompleter(this));
        getCommand("jails").setExecutor(new JailsCommand(this));
        getCommand("unjail").setExecutor(new UnjailCommand(this));
        getCommand("handcuff").setExecutor(new Handcuff(this));
        getCommand("unhandcuff").setExecutor(new HandcuffRemove(this));
        getCommand("jailsreload").setExecutor(new ConfigReload(this));
        getCommand("jailshelp").setExecutor(new JailsHelpCommand());
        getCommand("tempjail").setExecutor(new TempJailCommand(this));
        getCommand("jailsetflag").setExecutor(new SetFlag(this));
        getCommand("jaildelflag").setExecutor(new DelFlag(this));
        getCommand("jailflaglist").setExecutor(new FlagList(this));

        JailListCommand jailListCommand = new JailListCommand(this);
        getCommand("jailed").setExecutor(jailListCommand);
        getCommand("jailed").setTabCompleter(jailListCommand);


        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new JailListeners(this), this);
        getServer().getPluginManager().registerEvents(new CommandBlocker(this), this);

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
        try {
            jailLocationsConfig.save(jailLocationsFile);
        } catch (IOException e) {
            logger.severe("Could not save jail_locations.yml!");
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
        try {
            handcuffedPlayersConfig.save(handcuffedPlayersFile);
        } catch (IOException e) {
            logger.severe("Could not save handcuffed_players.yml!");
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

    public void sendJailsPlusMessage(Player sender){
        String message1 = JailsChatColor.GOLD + "You are trying to use a feature that is only available in Jails+.";
        sender.sendMessage(JailsChatColor.BOLD + message1);
        TextComponent message = new TextComponent("Click here to purchase Jails+!");
        message.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        message.setBold(true);
        message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, getPurchaseLink()));
        sender.spigot().sendMessage(message);
    }

    public String getPurchaseLink(){
        return purchaseLink;
    }
}