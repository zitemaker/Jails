package com.zitemaker.jail.translation;

import com.zitemaker.jail.JailPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class TranslationManager {
    private final JailPlugin plugin;
    private final Map<String, String> messages;
    private String currentLanguage;
    private final Map<String, String> defaultMessages = new HashMap<>();
    private final String[] supportedLanguages = {"en", "es", "fr", "ru", "de", "pt"};

    public TranslationManager(JailPlugin plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();

        plugin.reloadConfig();
        String lang = plugin.getConfig().getString("language", "en").toLowerCase();
        switch (lang) {
            case "english": lang = "en"; break;
            case "spanish": lang = "es"; break;
            case "french":  lang = "fr"; break;
            case "russian": lang = "ru"; break;
            case "german":  lang = "de"; break;
            case "portuguese": lang = "pt"; break;
            default:
        }
        if (Arrays.asList(supportedLanguages).contains(lang)) {
            currentLanguage = lang;
        } else {
            currentLanguage = "en";
            plugin.getLogger().warning("Invalid language in config: " + lang + ". Defaulting to English.");
        }

        loadLanguage(currentLanguage);
    }

    public void loadLanguage(String languageCode) {
        messages.clear();
        defaultMessages.clear();
        plugin.getLogger().info("Loading language: " + languageCode);

        InputStream defaultStream = plugin.getResource("lang/" + languageCode + ".yml");
        if (defaultStream == null) {
            plugin.getLogger().warning("Language file not found: " + languageCode);
            if (!languageCode.equals("en")) {
                loadLanguage("en");
            }
            return;
        }

        YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8));

        try {
            loadMessagesRecursively(langConfig, "messages");
            currentLanguage = languageCode;
            plugin.getLogger().info("Loaded " + messages.size() + " translations for '" + languageCode + "'");
            checkMissingTranslations();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading language '" + languageCode + "'", e);
            if (!languageCode.equals("en")) {
                loadLanguage("en");
            }
        }
    }

    private void loadMessagesRecursively(YamlConfiguration config, String path) {
        if (config.isConfigurationSection(path)) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection(path)).getKeys(false)) {
                String newPath = path + "." + key;
                if (config.isConfigurationSection(newPath)) {
                    loadMessagesRecursively(config, newPath);
                } else {
                    String value = config.getString(newPath);
                    if (value != null) {
                        messages.put(newPath, value);
                    } else {
                        plugin.getLogger().warning("Missing value for key: " + newPath);
                    }
                }
            }
        } else {
            String value = config.getString(path);
            if (value != null) {
                messages.put(path, value);
            } else {
                plugin.getLogger().warning("Missing value for path: " + path);
            }
        }
    }

    public String getMessage(String key) {
        String fullKey = key.startsWith("messages.") ? key : "messages." + key;
        String message = messages.get(fullKey);

        if (message == null) {
            plugin.getLogger().warning("Missing translation key: " + fullKey);
            return ChatColor.RED + "Missing translation key: " + fullKey + "\nDelete config/lang setting to restore defaults";
        }
        return message;
    }

    public void reloadMessages() {
        plugin.reloadConfig();
        String lang = plugin.getConfig().getString("language", "en").toLowerCase();
        switch (lang) {
            case "english": lang = "en"; break;
            case "spanish": lang = "es"; break;
            case "french":  lang = "fr"; break;
            case "russian": lang = "ru"; break;
            case "german":  lang = "de"; break;
            case "portuguese": lang = "pt"; break;
            default:
        }
        if (Arrays.asList(supportedLanguages).contains(lang)) {
            currentLanguage = lang;
        } else {
            currentLanguage = "en";
            plugin.getLogger().warning("Invalid language in config: " + lang + ". Defaulting to English.");
        }
        loadLanguage(currentLanguage);
    }

    public void checkMissingTranslations() {
        for (String key : defaultMessages.keySet()) {
            if (!messages.containsKey(key)) {
                plugin.getLogger().warning("Missing translation for '" + currentLanguage + "' key: " + key);
            }
        }
    }

    public boolean setLanguage(String languageCode) {
        if (Arrays.asList(supportedLanguages).contains(languageCode)) {
            loadLanguage(languageCode);
            return true;
        }
        return false;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}