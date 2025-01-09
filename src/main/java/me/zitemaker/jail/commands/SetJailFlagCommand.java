package me.zitemaker.jail.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import me.zitemaker.jail.JailPlugin;
import me.zitemaker.jail.utils.JailFlagManager;

import java.util.ArrayList;
import java.util.List;

public class SetJailFlagCommand implements CommandExecutor, TabCompleter {
    private final JailPlugin plugin;
    private final JailFlagManager flagManager;

    public SetJailFlagCommand(JailPlugin plugin) {
        this.plugin = plugin;
        this.flagManager = new JailFlagManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage("§cUsage: /jail setflag <name> <position>");
            return true;
        }

        String flagName = args[1];
        Location location = player.getLocation();

        flagManager.setJailFlag(flagName, location);
        player.sendMessage("§aJail flag '" + flagName + "' has been set at your current location!");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("setflag");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setflag")) {
            // Add existing flag names for tab completion
            completions.addAll(flagManager.getExistingFlagNames());
        }

        return completions;
    }
}