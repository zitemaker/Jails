package me.zitemaker.jail.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JailsHelpCommand implements CommandExecutor {

    private static final String PREFIX = "§b";
    private static final String HIGHLIGHT = "§e";
    private static final String ERROR = "§c";

    private final Map<Integer, String[]> helpPages;

    public JailsHelpCommand() {
        helpPages = new HashMap<>();
        initializeHelpPages();
    }

    private void initializeHelpPages() {
        helpPages.put(1, new String[]{
                "Jail Management Core Commands:",
                "/jailshelp - Display comprehensive help guide",
                "/jailsreload - Reload configuration without server restart",
                "/jail <player> <jail name> [reason] - Jail a player",
                "/jailduration [player] - Check player's current jail duration",
                "/tempjail <player> <jail name> <duration> [reason] - Temporarily jail a player"
        });

        helpPages.put(2, new String[]{
                "Jail Location and Player Management:",
                "/jailset <jail name> - Set jail location at current position",
                "/deljail <jail name> - Remove a specific jail location",
                "/jails - Open jail management interface",
                "/jailed list - View all currently jailed players",
                "/unjail <player name> - Release a player from jail"
        });

        helpPages.put(3, new String[]{
                "Advanced Jail and Handcuff Commands:",
                "/handcuff <player> - Handcuff a player",
                "/unhandcuff <player> - Unhandcuff a player",
                "/jailspawn <player> <spawn|originallocation> - Set unjail teleport point",
                "/jailsetflag <name> - Define jail area boundaries using WorldEdit",
                "/jaildelflag <name> - Remove a specific jail boundary flag",
                "/jailflaglist - View all defined jail boundary flags"
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int page = determineRequestedPage(args);

        if (!isValidPage(page)) {
            sender.sendMessage(ERROR + "Invalid page number. Available pages: 1-" + helpPages.size());
            return true;
        }

        displayHelpPage(sender, page);
        return true;
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
        sender.sendMessage(PREFIX + "━━━━━━━━━━━━━━━━━━━━ [Help (" + page + " of " + helpPages.size() + ")] ━━━━━━━━━━━━━━━━━━━━");

        for (String line : helpPages.get(page)) {
            sender.sendMessage(HIGHLIGHT + line);
        }
        sender.sendMessage("");

        sendNavigationButtons(sender, page);
    }

    private void sendNavigationButtons(CommandSender sender, int page) {
        TextComponent navButtons = new TextComponent(PREFIX + "━━━━━━━━━━━━━━━━━━━━━ ");

        addPreviousPageButton(navButtons, page);
        addNextPageButton(navButtons, page);

        navButtons.addExtra(PREFIX + " ━━━━━━━━━━━━━━━━━━━━━━");
        sender.spigot().sendMessage(navButtons);
    }

    private void addPreviousPageButton(TextComponent navButtons, int page) {
        if (page > 1) {
            TextComponent prevButton = new TextComponent("§a<< ");
            prevButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§7Previous Help Page").create()));
            prevButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/jailshelp " + (page - 1)));
            navButtons.addExtra(prevButton);
        } else {
            navButtons.addExtra("§7<< ");
        }
    }

    private void addNextPageButton(TextComponent navButtons, int page) {
        if (page < helpPages.size()) {
            TextComponent nextButton = new TextComponent("§a>>");
            nextButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§7Next Help Page").create()));
            nextButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/jailshelp " + (page + 1)));
            navButtons.addExtra(nextButton);
        } else {
            navButtons.addExtra("§7>>");
        }
    }
}