package io.papermc.paper.registry;

import org.bukkit.Keyed;
import org.bukkit.Registry;

public interface RegistryAccess {
    static RegistryAccess registryAccess() { throw new UnsupportedOperationException(); }
    <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> registryKey);
}
