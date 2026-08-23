package dev.scripted.essentials.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import dev.scripted.essentials.config.YamlDocument;

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

    public static void write(YamlDocument document, String path, Location location) {
        document.set(path + ".world", location.getWorld().getName());
        document.set(path + ".x", location.getX());
        document.set(path + ".y", location.getY());
        document.set(path + ".z", location.getZ());
        document.set(path + ".yaw", (double) location.getYaw());
        document.set(path + ".pitch", (double) location.getPitch());
    }

    /** Returns null when the entry is missing or names a world that is not loaded. */
    public static Location read(YamlDocument document, String path) {
        String worldName = document.getString(path + ".world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world,
                document.getDouble(path + ".x"),
                document.getDouble(path + ".y"),
                document.getDouble(path + ".z"),
                (float) document.getDouble(path + ".yaw"),
                (float) document.getDouble(path + ".pitch"));
    }

    /** A short "world 120, 64, -310" description for messages and menus. */
    public static String describe(Location location) {
        return location.getWorld().getName() + " "
                + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }
}
