package me.zitemaker.jail.utils;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class JailUtils {
    private final Map<String, Location> jailCells = new HashMap<>();

    public void setJailCell(String cellName, Location location) {
        jailCells.put(cellName, location);
    }

    public Location getJailCellLocation(String cellName) {
        return jailCells.get(cellName);
    }

    public String getDefaultJail() {
        return jailCells.keySet().stream().findFirst().orElse(null);
    }

    public List<String> getAllJailCells() {
        return new ArrayList<>(jailCells.keySet());
    }
}