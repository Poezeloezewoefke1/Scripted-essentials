package org.bukkit.configuration;

public class MemoryConfiguration implements Configuration {
    @Override public java.util.Set<String> getKeys(boolean deep) { throw new UnsupportedOperationException(); }
    @Override public java.util.Map<String, Object> getValues(boolean deep) { throw new UnsupportedOperationException(); }
    @Override public boolean contains(String path) { throw new UnsupportedOperationException(); }
    @Override public boolean isSet(String path) { throw new UnsupportedOperationException(); }
    @Override public Object get(String path) { throw new UnsupportedOperationException(); }
    @Override public Object get(String path, Object def) { throw new UnsupportedOperationException(); }
    @Override public void set(String path, Object value) { }
    @Override public ConfigurationSection createSection(String path) { throw new UnsupportedOperationException(); }
    @Override public ConfigurationSection getConfigurationSection(String path) { throw new UnsupportedOperationException(); }
    @Override public String getString(String path) { throw new UnsupportedOperationException(); }
    @Override public String getString(String path, String def) { throw new UnsupportedOperationException(); }
    @Override public int getInt(String path) { throw new UnsupportedOperationException(); }
    @Override public int getInt(String path, int def) { throw new UnsupportedOperationException(); }
    @Override public boolean getBoolean(String path) { throw new UnsupportedOperationException(); }
    @Override public boolean getBoolean(String path, boolean def) { throw new UnsupportedOperationException(); }
    @Override public double getDouble(String path) { throw new UnsupportedOperationException(); }
    @Override public double getDouble(String path, double def) { throw new UnsupportedOperationException(); }
    @Override public long getLong(String path) { throw new UnsupportedOperationException(); }
    @Override public long getLong(String path, long def) { throw new UnsupportedOperationException(); }
    @Override public java.util.List<String> getStringList(String path) { throw new UnsupportedOperationException(); }
    @Override public java.util.List<Integer> getIntegerList(String path) { throw new UnsupportedOperationException(); }
    @Override public java.util.List<?> getList(String path) { throw new UnsupportedOperationException(); }
    @Override public org.bukkit.Location getLocation(String path) { throw new UnsupportedOperationException(); }
    @Override public org.bukkit.inventory.ItemStack getItemStack(String path) { throw new UnsupportedOperationException(); }
    @Override public org.bukkit.inventory.ItemStack getItemStack(String path, org.bukkit.inventory.ItemStack def) { throw new UnsupportedOperationException(); }
    @Override public void setDefaults(Configuration defaults) { }
    @Override public Configuration getDefaults() { throw new UnsupportedOperationException(); }
}
