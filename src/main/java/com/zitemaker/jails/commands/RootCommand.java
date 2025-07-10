package com.zitemaker.jails.commands;

import com.zitemaker.jails.JailsFree;
import com.zitemaker.jails.interfaces.SubCommandExecutor;
import com.zitemaker.jails.commands.subcommands.*;
import com.zitemaker.jails.update.JailsVersionCommand;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RootCommand implements CommandExecutor {
    private final JailsFree plugin;
    @Getter
    private final Map<String, SubCommandExecutor> subCommands = new HashMap<>();

    public RootCommand(JailsFree plugin) {
        this.plugin = plugin;
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("unjail", new UnjailSC(plugin));
        subCommands.put("deljail", new DeleteJailSC(plugin));
        subCommands.put("handcuff", new HandcuffSC(plugin));
        subCommands.put("unhandcuff", new UnHandcuffSC(plugin));
        subCommands.put("setjail", new JailSetSC(plugin));
        subCommands.put("duration", new JailDurationSC(plugin));
        subCommands.put("help", new JailsHelpSC(plugin));
        subCommands.put("version", new JailsVersionCommand(plugin));
        subCommands.put("tempjail", new TempJailSC(plugin));
        subCommands.put("jail", new JailSC(plugin));
        subCommands.put("jailslist", new JailsSC(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            subCommands.get("help").onSubCommand(sender, new String[0]);
            return true;
        }

        SubCommandExecutor sub = subCommands.get(args[0].toLowerCase());
        if (sub != null) {
            sub.onSubCommand(sender, Arrays.copyOfRange(args, 1, args.length));
        } else {
            sender.sendMessage(plugin.getTranslationManager().getMessage("unknown_command"));
        }
        return true;
    }
}