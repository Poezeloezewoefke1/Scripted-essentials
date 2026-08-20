package org.bukkit;

import org.bukkit.plugin.Plugin;

public final class NamespacedKey {
    public NamespacedKey(Plugin plugin, String key) { }
    public static NamespacedKey minecraft(String key) { throw new UnsupportedOperationException(); }
    public String getKey() { throw new UnsupportedOperationException(); }
    public String getNamespace() { throw new UnsupportedOperationException(); }
}
