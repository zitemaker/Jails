/*
 * Jails
 * Copyright (C) 2025 Zitemaker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zitemaker.jails.integration;

import com.zitemaker.jails.JailsFree;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class JailsPlaceholderExpansion extends PlaceholderExpansion {
    private final JailsFree plugin;

    public JailsPlaceholderExpansion(JailsFree plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "jails";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Zitemaker";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        FileConfiguration jailedConfig = plugin.getJailedPlayersConfig();

        if (player != null) {
            UUID playerUUID = player.getUniqueId();
            String basePath = playerUUID.toString();

            switch (params.toLowerCase()) {
                case "isjailed":
                    return String.valueOf(plugin.isPlayerJailed(playerUUID));

                case "ishandcuffed":
                    return String.valueOf(plugin.isPlayerHandcuffed(playerUUID));

                case "jailname":
                    if (!plugin.isPlayerJailed(playerUUID)) return "";
                    return jailedConfig.getString(basePath + ".jailName", "");

                case "reason":
                    if (!plugin.isPlayerJailed(playerUUID)) return "";
                    String reason = jailedConfig.getString(basePath + ".reason", "");
                    return reason.isEmpty() ? "No reason" : reason;

                case "jailer":
                    if (!plugin.isPlayerJailed(playerUUID)) return "";
                    return jailedConfig.getString(basePath + ".jailer", "Unknown");

                case "jail_status":
                    if (!plugin.isPlayerJailed(playerUUID)) return "FREE";
                    long endTime = jailedConfig.getLong(basePath + ".endTime", 0);
                    if (endTime == -1) return "PERMANENT";
                    if (System.currentTimeMillis() >= endTime) return "EXPIRED";
                    return "JAILED";

                case "spawn_option":
                    return plugin.getPlayerSpawnOption(playerUUID);

                case "timeleft":
                    if (!plugin.isPlayerJailed(playerUUID)) return "";
                    long end = jailedConfig.getLong(basePath + ".endTime", 0);
                    if (end == -1) return "PERMANENT";
                    long remaining = end - System.currentTimeMillis();
                    if (remaining <= 0) return "Expired";
                    return formatTimeLeft(remaining);

                case "timeleft_seconds":
                    if (!plugin.isPlayerJailed(playerUUID)) return "0";
                    long endSec = jailedConfig.getLong(basePath + ".endTime", 0);
                    if (endSec == -1) return "-1";
                    long remainingSec = (endSec - System.currentTimeMillis()) / 1000;
                    return String.valueOf(Math.max(0, remainingSec));

                case "timeleft_minutes":
                    if (!plugin.isPlayerJailed(playerUUID)) return "0";
                    long endMin = jailedConfig.getLong(basePath + ".endTime", 0);
                    if (endMin == -1) return "-1";
                    long remainingMin = (endMin - System.currentTimeMillis()) / 1000 / 60;
                    return String.valueOf(Math.max(0, remainingMin));

                case "timeleft_hours":
                    if (!plugin.isPlayerJailed(playerUUID)) return "0";
                    long endHour = jailedConfig.getLong(basePath + ".endTime", 0);
                    if (endHour == -1) return "-1";
                    long remainingHour = (endHour - System.currentTimeMillis()) / 1000 / 60 / 60;
                    return String.valueOf(Math.max(0, remainingHour));

                case "timeleft_days":
                    if (!plugin.isPlayerJailed(playerUUID)) return "0";
                    long endDay = jailedConfig.getLong(basePath + ".endTime", 0);
                    if (endDay == -1) return "-1";
                    long remainingDay = (endDay - System.currentTimeMillis()) / 1000 / 60 / 60 / 24;
                    return String.valueOf(Math.max(0, remainingDay));

                case "endtime_timestamp":
                    if (!plugin.isPlayerJailed(playerUUID)) return "0";
                    return String.valueOf(jailedConfig.getLong(basePath + ".endTime", 0));

                case "jailtime_percentage":
                    if (!plugin.isPlayerJailed(playerUUID)) return "0";
                    long endPerc = jailedConfig.getLong(basePath + ".endTime", 0);
                    if (endPerc == -1) return "0";
                    long startTime = jailedConfig.getLong(basePath + ".startTime", System.currentTimeMillis());
                    long totalDuration = endPerc - startTime;
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (totalDuration <= 0) return "0";
                    int percentage = (int) ((elapsed * 100) / totalDuration);
                    return String.valueOf(Math.min(100, Math.max(0, percentage)));

                case "jail_location":
                    if (!plugin.isPlayerJailed(playerUUID)) return "";
                    String jailName = jailedConfig.getString(basePath + ".jailName", "");
                    Location jailLoc = plugin.getJail(jailName);
                    if (jailLoc == null) return "";
                    return String.format("%s %.0f %.0f %.0f",
                        jailLoc.getWorld() != null ? jailLoc.getWorld().getName() : "unknown",
                        jailLoc.getX(), jailLoc.getY(), jailLoc.getZ());

                case "jail_location_world":
                    if (!plugin.isPlayerJailed(playerUUID)) return "";
                    String jName = jailedConfig.getString(basePath + ".jailName", "");
                    Location jLoc = plugin.getJail(jName);
                    if (jLoc == null || jLoc.getWorld() == null) return "";
                    return jLoc.getWorld().getName();

                case "jail_location_x":
                    if (!plugin.isPlayerJailed(playerUUID)) return "";
                    String jnX = jailedConfig.getString(basePath + ".jailName", "");
                    Location jLocX = plugin.getJail(jnX);
                    if (jLocX == null) return "";
                    return String.valueOf((int) jLocX.getX());

                case "jail_location_y":
                    if (!plugin.isPlayerJailed(playerUUID)) return "";
                    String jnY = jailedConfig.getString(basePath + ".jailName", "");
                    Location jLocY = plugin.getJail(jnY);
                    if (jLocY == null) return "";
                    return String.valueOf((int) jLocY.getY());

                case "jail_location_z":
                    if (!plugin.isPlayerJailed(playerUUID)) return "";
                    String jnZ = jailedConfig.getString(basePath + ".jailName", "");
                    Location jLocZ = plugin.getJail(jnZ);
                    if (jLocZ == null) return "";
                    return String.valueOf((int) jLocZ.getZ());

                case "original_location":
                    if (!plugin.isPlayerJailed(playerUUID)) return "";
                    String world = jailedConfig.getString(basePath + ".original.world", "");
                    double x = jailedConfig.getDouble(basePath + ".original.x", 0);
                    double y = jailedConfig.getDouble(basePath + ".original.y", 0);
                    double z = jailedConfig.getDouble(basePath + ".original.z", 0);
                    return String.format("%s %.0f %.0f %.0f", world, x, y, z);

                case "original_location_world":
                    if (!plugin.isPlayerJailed(playerUUID)) return "";
                    return jailedConfig.getString(basePath + ".original.world", "");


            }
        }

        switch (params.toLowerCase()) {
            case "totalcount":
                return String.valueOf(jailedConfig.getKeys(false).size());

            case "handcuffed_count":
                return String.valueOf(plugin.getHandcuffedPlayerNames().size());

            case "jail_count":
                return String.valueOf(plugin.getJails().size());


            case "jails_list":
                return String.join("\n", plugin.getJails().keySet());

            case "jails_list_comma":
                return String.join(", ", plugin.getJails().keySet());

            case "jailed_players_list":
                return String.join("\n", plugin.getJailedPlayerNames());

            case "jailed_players_list_comma":
                return String.join(", ", plugin.getJailedPlayerNames());

            case "handcuffed_players_list":
                return String.join("\n", plugin.getHandcuffedPlayerNames());

            case "handcuffed_players_list_comma":
                return String.join(", ", plugin.getHandcuffedPlayerNames());
        }

        if (params.toLowerCase().startsWith("jail_") && params.toLowerCase().endsWith("_count")) {
            String jailName = params.substring(5, params.length() - 6);
            int count = 0;
            for (String key : jailedConfig.getKeys(false)) {
                String jName = jailedConfig.getString(key + ".jailName", "");
                if (jName.equalsIgnoreCase(jailName)) {
                    count++;
                }
            }
            return String.valueOf(count);
        }

        return null;
    }

    private String formatTimeLeft(long millis) {
        long totalSeconds = millis / 1000;

        long days = totalSeconds / (24 * 3600);
        totalSeconds %= (24 * 3600);

        long hours = totalSeconds / 3600;
        totalSeconds %= 3600;

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        StringBuilder result = new StringBuilder();

        if (days > 0) {
            result.append(days).append(" day").append(days > 1 ? "s" : "");
        }
        if (hours > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(hours).append(" hour").append(hours > 1 ? "s" : "");
        }
        if (minutes > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
        }
        if (seconds > 0 && days == 0 && hours == 0) {
            if (result.length() > 0) result.append(", ");
            result.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
        }

        return result.length() > 0 ? result.toString() : "0 seconds";
    }
}
