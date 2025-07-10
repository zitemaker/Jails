package com.zitemaker.jails.translation;

import com.zitemaker.jails.JailsFree;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class TranslationManager {

    private final JailsFree plugin;
    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, String> defaultMessages = new HashMap<>();
    private final String[] supportedLanguages = {"en", "es", "ru", "fr", "de", "pt", "pl"};
    @Getter
    private String currentLanguage;

    public TranslationManager(JailsFree plugin) {
        this.plugin = plugin;

        String lang = plugin.getLang();

        lang = switch (lang.toLowerCase()) {
            case "english" -> "en";
            case "spanish" -> "es";
            case "russian" -> "ru";
            case "french" -> "fr";
            case "german" -> "de";
            case "portuguese" -> "pt";
            case "polish" -> "pl";
            default -> lang;
        };

        if (Arrays.asList(supportedLanguages).contains(lang)) {
            currentLanguage = lang;
        } else {
            currentLanguage = "en";
            plugin.getLogger().warning("Invalid language in config: " + lang + ". Defaulting to English.");
        }

        loadLanguage(currentLanguage);
    }

    public void restoreLanguageFiles() {
        for (String lang : supportedLanguages) {
            File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
            if (!langFile.exists() || langFile.length() == 0) {
                plugin.getLogger().info("Restoring missing or empty language file: " + lang + ".yml");
                plugin.saveResource("lang/" + lang + ".yml", true);
            }
        }
    }

    public void loadLanguage(String languageCode) {
        messages.clear();
        defaultMessages.clear();
        plugin.getLogger().info("Loading language: " + languageCode);

        try (InputStream defaultEnStream = plugin.getResource("lang/en.yml")) {
            if (defaultEnStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultEnStream, StandardCharsets.UTF_8));
                loadMessages(defaultConfig, "messages", defaultMessages);
            } else {
                plugin.getLogger().severe("Default language file (en.yml) not found in JAR!");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading default language file (en.yml)", e);
        }

        YamlConfiguration langConfig;
        try (InputStream langStream = plugin.getResource("lang/" + languageCode + ".yml")) {
            if (langStream == null) {
                plugin.getLogger().warning("Language file not found in JAR: " + languageCode);
                if (!languageCode.equals("en")) {
                    loadLanguage("en");
                }
                return;
            }
            langConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(langStream, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading language file: " + languageCode, e);
            if (!languageCode.equals("en")) {
                loadLanguage("en");
            }
            return;
        }

        try {
            loadMessages(langConfig, "messages", messages);
            currentLanguage = languageCode;
            plugin.getLogger().info("Loaded " + messages.size() + " translations for '" + languageCode + "'");
            checkMissingTranslations();
            LangNotice.create(plugin);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading language '" + languageCode + "'", e);
            if (!languageCode.equals("en")) {
                loadLanguage("en");
            }
        }
    }

    private void loadMessages(YamlConfiguration config, String path, Map<String, String> target) {
        if (config.isConfigurationSection(path)) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection(path)).getKeys(false)) {
                String newPath = path + "." + key;
                if (config.isConfigurationSection(newPath)) {
                    loadMessages(config, newPath, target);
                } else {
                    String value = config.getString(newPath);
                    if (value != null) {
                        target.put(newPath, value);
                    } else {
                        plugin.getLogger().warning("Missing value for key: " + newPath);
                    }
                }
            }
        } else {
            String value = config.getString(path);
            if (value != null) {
                target.put(path, value);
            } else {
                plugin.getLogger().warning("Missing value for path: " + path);
            }
        }
    }

    public String getMessage(String key) {
        String fullKey = key.startsWith("messages.") ? key : "messages." + key;
        String message = messages.get(fullKey);

        if (message == null) {
            message = defaultMessages.get(fullKey);
            if (message == null) {
                plugin.getLogger().warning("Missing translation keys: " + fullKey);
                return ChatColor.RED + "Missing translation key: " + fullKey + "\nDelete .yml and reload to restore defaults";
            }
        }

        message = message.replace("{prefix}", plugin.getPrefix());
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void reloadMessages() {
        String lang = plugin.getConfig().getString("language", "en").toLowerCase();

        lang = switch (lang) {
            case "english" -> "en";
            case "spanish" -> "es";
            case "russian" -> "ru";
            case "french" -> "fr";
            case "german" -> "de";
            case "portuguese" -> "pt";
            case "polish" -> "pl";
            default -> lang;
        };

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
}