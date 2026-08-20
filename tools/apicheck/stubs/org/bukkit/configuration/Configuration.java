package org.bukkit.configuration;

public interface Configuration extends ConfigurationSection {
    void setDefaults(Configuration defaults);
    Configuration getDefaults();
}
