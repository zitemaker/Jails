package me.zitemaker.jail.flags;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import me.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class SetFlag implements CommandExecutor {

    private final JailPlugin plugin;
    private final File flagsFile;
    private final FileConfiguration flagsConfig;

    public SetFlag(JailPlugin plugin) {
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

        plugin.getCommand("jailsetflag").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /jailsetflag <name>");
            return true;
        }

        String flagName = args[0];

        WorldEditPlugin worldEdit = (WorldEditPlugin) plugin.getServer().getPluginManager().getPlugin("WorldEdit");
        if (worldEdit == null) {
            player.sendMessage(ChatColor.RED + "WorldEdit is not installed on this server!");
            return true;
        }

        Region selection;
        try {
            selection = worldEdit.getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));
            if (selection == null) {
                player.sendMessage(ChatColor.RED + "You must make a selection with WorldEdit first!");
                return true;
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to get your WorldEdit selection!");
            return true;
        }

        BlockVector3 min = selection.getMinimumPoint();
        BlockVector3 max = selection.getMaximumPoint();

        flagsConfig.set(flagName + ".world", player.getWorld().getName());
        flagsConfig.set(flagName + ".pos1", min.getBlockX() + "," + min.getBlockY() + "," + min.getBlockZ());
        flagsConfig.set(flagName + ".pos2", max.getBlockX() + "," + max.getBlockY() + "," + max.getBlockZ());

        try {
            flagsConfig.save(flagsFile);
            player.sendMessage(ChatColor.GREEN + "Flag '" + flagName + "' has been set successfully!");
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while saving the flag!");
            e.printStackTrace();
        }

        return true;
    }
}