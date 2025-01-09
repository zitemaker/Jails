package me.zitemaker.jail.commands;

import me.zitemaker.jail.utils.JailUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JailCellCommand implements CommandExecutor {
    private final JailUtils jailUtils;

    public JailCellCommand(JailUtils jailUtils) {
        this.jailUtils = jailUtils;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 3 || !args[0].equalsIgnoreCase("cell") || !args[2].equalsIgnoreCase("spawnset")) {
            player.sendMessage("Usage: /jail cell <cell_name> spawnset");
            return true;
        }

        String cellName = args[1];
        Location location = player.getLocation();

        jailUtils.setJailCell(cellName, location);
        player.sendMessage("Spawn point for cell '" + cellName + "' set at your current location.");

        return true;
    }
}