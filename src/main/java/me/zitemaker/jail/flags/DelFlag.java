package me.zitemaker.jail.flags;

import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DelFlag implements CommandExecutor, TabCompleter {

    private final JailPlugin plugin;
    private final File flagsFile;
    private final FileConfiguration flagsConfig;

    public DelFlag(JailPlugin plugin) {
        this.plugin = plugin;

        this.flagsFile = new File(plugin.getDataFolder(), "flags.yml");
        if (!flagsFile.exists()) {
            try {
                flagsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create flags.yml!");
                e.printStackTrace();
            }
        }
        this.flagsConfig = YamlConfiguration.loadConfiguration(flagsFile);

        plugin.getCommand("jaildelflag").setExecutor(this);
        plugin.getCommand("jaildelflag").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /jaildelflag <name>");
            return true;
        }

        String flagName = args[0];

        if (!flagsConfig.contains(flagName)) {
            sender.sendMessage(ChatColor.RED + "The flag '" + flagName + "' does not exist!");
            return true;
        }

        // Remove the flag from the configuration
        flagsConfig.set(flagName, null);

        try {
            flagsConfig.save(flagsFile);
            sender.sendMessage(ChatColor.GREEN + "Flag '" + flagName + "' has been deleted!");
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "An error occurred while deleting the flag!");
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Suggest flag names
            return new ArrayList<>(flagsConfig.getKeys(false));
        }

        return Collections.emptyList();
    }
}