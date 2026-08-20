package dev.scripted.essentials.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/** A YAML file inside the plugin's data folder, created on demand. */
public final class DataFile {

    private final Plugin plugin;
    private final File file;
    private YamlConfiguration configuration;

    public DataFile(Plugin plugin, String path) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), path);
        reload();
    }

    public YamlConfiguration get() {
        if (configuration == null) {
            reload();
        }
        return configuration;
    }

    public void reload() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Could not create directory " + parent.getPath());
        }
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("Could not create " + file.getName());
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not create " + file.getName(), e);
            }
        }
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        if (configuration == null) {
            return;
        }
        try {
            configuration.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getName(), e);
        }
    }
}
