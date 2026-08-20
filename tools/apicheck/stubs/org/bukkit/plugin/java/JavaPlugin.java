package org.bukkit.plugin.java;

import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

public abstract class JavaPlugin implements Plugin {
    public void onEnable() { }
    public void onDisable() { }
    public void onLoad() { }
    @Override public File getDataFolder() { throw new UnsupportedOperationException(); }
    @Override public FileConfiguration getConfig() { throw new UnsupportedOperationException(); }
    @Override public void saveConfig() { }
    @Override public void reloadConfig() { }
    @Override public void saveDefaultConfig() { }
    @Override public void saveResource(String resourcePath, boolean replace) { }
    @Override public InputStream getResource(String filename) { throw new UnsupportedOperationException(); }
    @Override public Logger getLogger() { throw new UnsupportedOperationException(); }
    @Override public Server getServer() { return Bukkit.getServer(); }
    @Override public String getName() { throw new UnsupportedOperationException(); }
    @Override public boolean isEnabled() { throw new UnsupportedOperationException(); }
    public PluginMeta getPluginMeta() { throw new UnsupportedOperationException(); }
}
