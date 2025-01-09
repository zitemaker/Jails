package me.zitemaker.jail;

import org.bukkit.plugin.java.JavaPlugin;
import me.zitemaker.jail.commands.SetJailFlagCommand;

public class JailPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Create the plugin directory if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Register the command
        SetJailFlagCommand setJailFlagCommand = new SetJailFlagCommand(this);
        getCommand("jail").setExecutor(setJailFlagCommand);
        getCommand("jail").setTabCompleter(setJailFlagCommand);
    }

    @Override
    public void onDisable() {
        getLogger().info("JailPlugin has been disabled!");
    }
}