package org.bukkit.persistence;

import org.bukkit.NamespacedKey;

import java.util.Set;

public interface PersistentDataContainer {
    <P, C> void set(NamespacedKey key, PersistentDataType<P, C> type, C value);
    <P, C> C get(NamespacedKey key, PersistentDataType<P, C> type);
    <P, C> C getOrDefault(NamespacedKey key, PersistentDataType<P, C> type, C defaultValue);
    <P, C> boolean has(NamespacedKey key, PersistentDataType<P, C> type);
    boolean has(NamespacedKey key);
    void remove(NamespacedKey key);
    boolean isEmpty();
    Set<NamespacedKey> getKeys();
}
