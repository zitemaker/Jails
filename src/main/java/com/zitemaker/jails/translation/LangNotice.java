package com.zitemaker.jails.translation;

import com.zitemaker.jails.JailsFree;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LangNotice {

    public static void create(JailsFree plugin) {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            boolean mkdirs = langDir.mkdirs();
        }

        File readme = new File(langDir, "README.txt");
        if (readme.exists()) return;

        try (FileWriter writer = new FileWriter(readme)) {
            writer.write("This folder contains built-in internal language support.\n");
            writer.write("\n");
            writer.write("You are using the free version of Jails+.\n");
            writer.write("All official language files are safely embedded inside the plugin.\n");
            writer.write("\n");
            writer.write("While you cannot edit them in this version, you can still select your preferred language\n");
            writer.write("by changing the 'language' option in the config.yml! e.g., en (english), es (spanish), fr (french), de (deustch), ru (russian).\n");
            writer.write("\n");
            writer.write("If you want to fully customize translations or add your own language,\n");
            writer.write("upgrade to Jails+ for full language editing support.\n");
            writer.write("\n");
            writer.write("""
                    Buy Jails+ at SpigotMC:
                    https://www.spigotmc.org/resources/jails-the-next-generation-punishment-system-%EF%B8%8F%EF%B8%8F%E2%9A%94.124623/
                    """);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not create lang/README.txt: " + e.getMessage());
        }
    }
}
