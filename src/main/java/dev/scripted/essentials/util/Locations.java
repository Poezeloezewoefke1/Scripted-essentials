package dev.scripted.essentials.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Reads and writes locations as plain YAML keys.
 *
 * <p>Written field by field rather than through Bukkit's built-in serialization so that a warp
 * pointing at a world that is not currently loaded reads back as {@code null} instead of
 * throwing, and so the files stay hand-editable.
 */
public final class Locations {

    private Locations() {
    }

    public static void write(ConfigurationSection section, String path, Location location) {
        section.set(path + ".world", location.getWorld().getName());
        section.set(path + ".x", location.getX());
        section.set(path + ".y", location.getY());
        section.set(path + ".z", location.getZ());
        section.set(path + ".yaw", (double) location.getYaw());
        section.set(path + ".pitch", (double) location.getPitch());
    }

    /** Returns null when the entry is missing or names a world that is not loaded. */
    public static Location read(ConfigurationSection section, String path) {
        String worldName = section.getString(path + ".world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world,
                section.getDouble(path + ".x"),
                section.getDouble(path + ".y"),
                section.getDouble(path + ".z"),
                (float) section.getDouble(path + ".yaw"),
                (float) section.getDouble(path + ".pitch"));
    }

    /** A short "world 120, 64, -310" description for messages and menus. */
    public static String describe(Location location) {
        return location.getWorld().getName() + " "
                + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }
}
