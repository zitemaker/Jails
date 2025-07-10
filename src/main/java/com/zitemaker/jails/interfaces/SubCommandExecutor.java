package com.zitemaker.jails.interfaces;

import org.bukkit.command.CommandSender;

public interface SubCommandExecutor {
    void onSubCommand(CommandSender sender, String[] args);
}