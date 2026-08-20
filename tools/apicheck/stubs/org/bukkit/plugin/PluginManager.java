package org.bukkit.plugin;

import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;

public interface PluginManager {
    void registerEvents(Listener listener, Plugin plugin);
    void callEvent(Event event);
    Plugin getPlugin(String name);
    boolean isPluginEnabled(String name);
    void addPermission(Permission permission);
    void removePermission(Permission permission);
    void removePermission(String name);
    Permission getPermission(String name);
}
