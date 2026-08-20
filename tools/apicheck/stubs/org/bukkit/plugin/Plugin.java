package org.bukkit.plugin;

import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

public interface Plugin {
    File getDataFolder();
    FileConfiguration getConfig();
    void saveConfig();
    void reloadConfig();
    void saveDefaultConfig();
    void saveResource(String resourcePath, boolean replace);
    InputStream getResource(String filename);
    Logger getLogger();
    Server getServer();
    String getName();
    boolean isEnabled();
}
