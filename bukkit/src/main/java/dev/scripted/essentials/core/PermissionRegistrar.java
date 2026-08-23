package dev.scripted.essentials.core;

import dev.scripted.essentials.ScriptedEssentials;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers every feature's permission node with the server.
 *
 * <p>This is not cosmetic. Bukkit only expands a wildcard such as
 * {@code scriptedessentials.*} into the nodes underneath it if those nodes are registered as
 * children of it — an unregistered wildcard grants nothing at all. Registering them here is what
 * makes {@code scriptedessentials.*} work, and it is also how permission plugins like LuckPerms
 * discover the node list for tab completion.
 */
public final class PermissionRegistrar {

    private static final String WILDCARD = "scriptedessentials.*";

    private final ScriptedEssentials plugin;
    private final List<Permission> registered = new ArrayList<>();

    public PermissionRegistrar(ScriptedEssentials plugin) {
        this.plugin = plugin;
    }

    /** Call once, after every feature has been registered. */
    public void registerAll() {
        PluginManager manager = plugin.getServer().getPluginManager();
        Map<String, Boolean> children = new LinkedHashMap<>();

        for (Feature feature : plugin.features().all()) {
            String node = feature.definition().permission();
            children.put(node, true);
            add(manager, new Permission(node,
                    feature.definition().displayName() + " - " + feature.definition().description(),
                    PermissionDefault.OP));
        }
        add(manager, new Permission(WILDCARD, "Every ScriptedEssentials feature.",
                PermissionDefault.OP, children));
    }

    /** Call on shutdown, so a reload does not trip over nodes left behind by the last load. */
    public void unregisterAll() {
        PluginManager manager = plugin.getServer().getPluginManager();
        for (Permission permission : registered) {
            manager.removePermission(permission);
        }
        registered.clear();
    }

    private void add(PluginManager manager, Permission permission) {
        // A node can already exist if another plugin declared it, or if a previous load left it
        // behind; leave that one alone rather than failing to enable over it.
        if (manager.getPermission(permission.getName()) != null) {
            return;
        }
        manager.addPermission(permission);
        registered.add(permission);
    }
}
