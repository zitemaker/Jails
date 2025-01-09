package me.zitemaker.jail;

import me.zitemaker.jail.commands.JailCellCommand;
import me.zitemaker.jail.commands.JailCellTabCompleter;
import me.zitemaker.jail.commands.JailCommand;
import me.zitemaker.jail.commands.JailTabCompleter;
import me.zitemaker.jail.utils.JailUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class JailPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("JailPlugin enabled!");

        JailUtils jailUtils = new JailUtils();

        // Register commands
        getCommand("jail").setExecutor(new JailCommand(jailUtils));
        getCommand("jailcell").setExecutor(new JailCellCommand(jailUtils));

        // Register tab completers
        getCommand("jail").setTabCompleter(new JailTabCompleter(jailUtils));
        getCommand("jailcell").setTabCompleter(new JailCellTabCompleter());
    }

    @Override
    public void onDisable() {
        getLogger().info("JailPlugin disabled!");
    }
}