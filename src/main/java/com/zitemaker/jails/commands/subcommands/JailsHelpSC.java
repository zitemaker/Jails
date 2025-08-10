package com.zitemaker.jails.commands.subcommands;

import com.zitemaker.jails.JailsFree;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import com.zitemaker.jails.interfaces.SubCommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class JailsHelpSC implements SubCommandExecutor {
    private static final String PREFIX = "§9§l";
    private static final String COMMAND = "§b";
    private static final String DESCRIPTION = "§7";
    private static final String HEADER = "§3§l";
    private static final String FOOTER = "§8";

    private final JailsFree plugin;
    private final Map<Integer, String[]> helpPages;

    public JailsHelpSC(JailsFree plugin) {
        this.plugin = plugin;
        this.helpPages = new HashMap<>();
        initializeHelpPages();
    }

    @Override
    public void onSubCommand(@NotNull CommandSender sender, String[] args) {
        String prefix = plugin.getPrefix();

        if (!sender.hasPermission("jails.help")) {
            sender.sendMessage(plugin.getTranslationManager().getMessage("cr_no_permission").replace("{prefix}", prefix));
            return;
        }

        int page = determineRequestedPage(args);

        if (!isValidPage(page)) {
            sender.sendMessage(FOOTER + "Invalid page number. Available pages: 1-" + helpPages.size());
            return;
        }

        displayHelpPage(sender, page);
    }

    private void initializeHelpPages() {
        helpPages.put(1, new String[]{
                "Jail Management Core Commands:",
                "/jails help - Display comprehensive help guide",
                "/jailsreload - Reload configuration without server restart",
                "/jails <player> <jails name> [reason] - Jail a player",
        });

        helpPages.put(2, new String[]{
                "Jail Location and Player Management:",
                "/jails setjail <jails name> - Set jails location at current position",
                "/jails deljail <jails name> - Remove a specific jails location",
                "/jails jailslist - Open jails management interface",
                "/jails unjail <player name> - Release a player from jails"
        });

        helpPages.put(3, new String[]{
                "Advanced Jail and Handcuff Commands:",
                "/jails handcuff <player> - Handcuff a player",
                "/jails unhandcuff <player> - Unhandcuff a player",
        });
    }

    private int determineRequestedPage(String[] args) {
        if (args.length > 0) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private boolean isValidPage(int page) {
        return page > 0 && page <= helpPages.size();
    }

    private void displayHelpPage(CommandSender sender, int page) {
        sender.sendMessage(HEADER + "➤ Jail Management Help " +
                PREFIX + "(" + page + "/" + helpPages.size() + ") " + FOOTER + "»»»");

        for (String line : helpPages.get(page)) {
            if (line.contains(":")) {
                sender.sendMessage(HEADER + "◆ " + line);
            } else {
                String[] parts = line.split(" - ", 2);
                sender.sendMessage(COMMAND + parts[0] +
                        DESCRIPTION + " - " + parts[1]);
            }
        }

        sender.sendMessage(FOOTER + "════════════════════════════════");

        sendNavigationButtons(sender, page);
    }

    private void sendNavigationButtons(CommandSender sender, int page) {
        TextComponent navButtons = new TextComponent(PREFIX + "« ");

        if (page > 1) {
            TextComponent prevButton = new TextComponent("§a⬅ Previous");
            prevButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§7Click to view previous help page").create()));
            prevButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/jails help " + (page - 1)));
            navButtons.addExtra(prevButton);
        }

        if (page < helpPages.size()) {
            navButtons.addExtra(" §7| ");
            TextComponent nextButton = new TextComponent("§aNext ➡");
            nextButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§7Click to view next help page").create()));
            nextButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/jails help " + (page + 1)));
            navButtons.addExtra(nextButton);
        }

        sender.spigot().sendMessage(navButtons);
    }
}