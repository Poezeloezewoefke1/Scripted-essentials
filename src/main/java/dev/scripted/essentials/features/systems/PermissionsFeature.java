package dev.scripted.essentials.features.systems;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.CommandException;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A small permission system: groups with inheritance, plus per-player grants.
 *
 * <p>Permissions are applied through a Bukkit {@link PermissionAttachment}, which means every
 * other plugin's {@code hasPermission} checks see them without knowing this exists. Prefixing a
 * node with {@code -} denies it, and a denial always wins over a grant, so a group can hand out a
 * broad wildcard and still carve one command back out.
 *
 * <p>This is meant for servers that do not already run a permissions plugin. If LuckPerms is
 * installed, leave this feature switched off and let LuckPerms own permissions instead.
 */
public final class PermissionsFeature extends Feature {

    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private DataFile store;

    public PermissionsFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("permissions")
                .name("Custom Permission System")
                .description("Groups with inheritance and per-player grants. Leave off if you run LuckPerms.")
                .icon(Material.NETHER_STAR)
                .category(FeatureCategory.SYSTEMS)
                .controls("/seperm")
                .disabledByDefault()
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/permissions.yml");
        if (!store.get().contains("default-group")) {
            store.get().set("default-group", "default");
            store.get().set("groups.default.permissions", new ArrayList<String>());
            store.get().set("groups.default.inherits", new ArrayList<String>());
            store.save();
        }

        command(new SECommand(plugin, "seperm", "permissions") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/seperm <group|user|reload> ..."));
                }
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "group" -> group(sender, args);
                    case "user" -> user(sender, args);
                    case "reload" -> {
                        store.reload();
                        Bukkit.getOnlinePlayers().forEach(PermissionsFeature.this::apply);
                        plugin.messages().send(sender, "perm-reloaded");
                    }
                    default -> throw fail("usage",
                            Text.placeholder("usage", "/seperm <group|user|reload> ..."));
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(args[0], List.of("group", "user", "reload"));
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("group")) {
                    return filter(args[1], List.of("create", "delete", "add", "remove", "inherit", "list", "info"));
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("user")) {
                    return filter(args[1], List.of("setgroup", "add", "remove", "info"));
                }
                if (args.length == 3 && args[0].equalsIgnoreCase("group")) {
                    return filter(args[2], groupNames());
                }
                if (args.length == 3 && args[0].equalsIgnoreCase("user")) {
                    return onlineNames(sender, args[2]);
                }
                if (args.length == 4 && args[0].equalsIgnoreCase("user")
                        && args[1].equalsIgnoreCase("setgroup")) {
                    return filter(args[3], groupNames());
                }
                return List.of();
            }
        }.permission("scriptedessentials.admin")
                .describe("Manage groups and permissions.", "/seperm <group|user|reload>"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onJoin(PlayerJoinEvent event) {
                if (isEnabled()) {
                    apply(event.getPlayer());
                }
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                PermissionAttachment attachment = attachments.remove(event.getPlayer().getUniqueId());
                if (attachment != null) {
                    attachment.remove();
                }
            }
        });
    }

    @Override
    protected void onEnable() {
        Bukkit.getOnlinePlayers().forEach(this::apply);
    }

    @Override
    protected void onDisable() {
        attachments.values().forEach(PermissionAttachment::remove);
        attachments.clear();
        Bukkit.getOnlinePlayers().forEach(Player::recalculatePermissions);
    }

    @Override
    protected void onReload() {
        if (isEnabled()) {
            Bukkit.getOnlinePlayers().forEach(this::apply);
        }
    }

    // ---- applying ---------------------------------------------------------------------------

    /** Rebuilds a player's attachment from their group and personal nodes. */
    public void apply(Player player) {
        PermissionAttachment existing = attachments.remove(player.getUniqueId());
        if (existing != null) {
            existing.remove();
        }
        PermissionAttachment attachment = player.addAttachment(plugin);

        // Grants first, then denials, so an explicit "-node" always has the final say.
        Set<String> nodes = effectiveNodes(player);
        for (String node : nodes) {
            if (!node.startsWith("-")) {
                attachment.setPermission(node, true);
            }
        }
        for (String node : nodes) {
            if (node.startsWith("-")) {
                attachment.setPermission(node.substring(1), false);
            }
        }
        attachments.put(player.getUniqueId(), attachment);
        player.recalculatePermissions();
    }

    /** Every node that applies to a player: their group chain, then their own grants. */
    public Set<String> effectiveNodes(Player player) {
        Set<String> nodes = new LinkedHashSet<>();
        collectGroup(groupOf(player), nodes, new HashSet<>());
        nodes.addAll(store.get().getStringList("users." + player.getUniqueId() + ".permissions"));
        return nodes;
    }

    /** Walks the inheritance chain, guarding against a group that (indirectly) inherits itself. */
    private void collectGroup(String group, Set<String> into, Set<String> seen) {
        if (group == null || !seen.add(group.toLowerCase(Locale.ROOT))) {
            return;
        }
        for (String parent : store.get().getStringList("groups." + group + ".inherits")) {
            collectGroup(parent.toLowerCase(Locale.ROOT), into, seen);
        }
        into.addAll(store.get().getStringList("groups." + group + ".permissions"));
    }

    public String groupOf(Player player) {
        return store.get().getString("users." + player.getUniqueId() + ".group",
                store.get().getString("default-group", "default"));
    }

    public List<String> groupNames() {
        ConfigurationSection section = store.get().getConfigurationSection("groups");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    // ---- subcommands ------------------------------------------------------------------------

    private void group(CommandSender sender, String[] args) {
        if (args.length < 3) {
            throw abort("usage", Text.placeholder("usage",
                    "/seperm group <create|delete|add|remove|inherit|list|info> <group> [node]"));
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        String group = args[2].toLowerCase(Locale.ROOT);

        switch (action) {
            case "create" -> {
                if (store.get().contains("groups." + group)) {
                    throw abort("perm-group-exists", Text.placeholder("group", group));
                }
                store.get().set("groups." + group + ".permissions", new ArrayList<String>());
                store.get().set("groups." + group + ".inherits", new ArrayList<String>());
                save(sender, "perm-group-created", Text.placeholder("group", group));
            }
            case "delete" -> {
                requireGroup(group);
                store.get().set("groups." + group, null);
                save(sender, "perm-group-deleted", Text.placeholder("group", group));
            }
            case "list" -> plugin.messages().send(sender, "perm-group-list",
                    Text.placeholder("groups", groupNames().isEmpty() ? "none" : String.join(", ", groupNames())));
            case "info" -> {
                requireGroup(group);
                plugin.messages().send(sender, "perm-group-info",
                        Text.placeholder("group", group),
                        Text.placeholder("inherits", String.join(", ",
                                store.get().getStringList("groups." + group + ".inherits"))),
                        Text.placeholder("permissions", String.join(", ",
                                store.get().getStringList("groups." + group + ".permissions"))));
            }
            case "add", "remove", "inherit" -> {
                requireGroup(group);
                if (args.length < 4) {
                    throw abort("usage", Text.placeholder("usage",
                            "/seperm group " + action + " " + group + " <value>"));
                }
                String value = args[3];
                String path = action.equals("inherit")
                        ? "groups." + group + ".inherits"
                        : "groups." + group + ".permissions";
                List<String> values = new ArrayList<>(store.get().getStringList(path));
                boolean removing = action.equals("remove");

                if (removing) {
                    if (!values.remove(value)) {
                        throw abort("perm-node-missing", Text.placeholder("node", value));
                    }
                } else {
                    if (values.contains(value)) {
                        throw abort("perm-node-exists", Text.placeholder("node", value));
                    }
                    values.add(value);
                }
                store.get().set(path, values);
                refreshAllOnline();
                save(sender, removing ? "perm-node-removed" : "perm-node-added",
                        Text.placeholder("node", value), Text.placeholder("group", group));
            }
            default -> throw abort("usage", Text.placeholder("usage",
                    "/seperm group <create|delete|add|remove|inherit|list|info> <group> [node]"));
        }
    }

    private void user(CommandSender sender, String[] args) {
        if (args.length < 3) {
            throw abort("usage", Text.placeholder("usage",
                    "/seperm user <setgroup|add|remove|info> <player> [value]"));
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        // Prefer the online player: getOfflinePlayer(String) can block on a profile lookup.
        Player online = Bukkit.getPlayerExact(args[2]);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(args[2]);
        if (target.getName() == null) {
            throw abort("player-not-found", Text.placeholder("player", args[2]));
        }
        String base = "users." + target.getUniqueId();
        store.get().set(base + ".name", target.getName());

        switch (action) {
            case "info" -> plugin.messages().send(sender, "perm-user-info",
                    Text.placeholder("player", target.getName()),
                    Text.placeholder("group", store.get().getString(base + ".group",
                            store.get().getString("default-group", "default"))),
                    Text.placeholder("permissions", String.join(", ",
                            store.get().getStringList(base + ".permissions"))));
            case "setgroup" -> {
                if (args.length < 4) {
                    throw abort("usage", Text.placeholder("usage",
                            "/seperm user setgroup " + target.getName() + " <group>"));
                }
                String group = args[3].toLowerCase(Locale.ROOT);
                requireGroup(group);
                store.get().set(base + ".group", group);
                refresh(target);
                save(sender, "perm-user-group-set",
                        Text.placeholder("player", target.getName()), Text.placeholder("group", group));
            }
            case "add", "remove" -> {
                if (args.length < 4) {
                    throw abort("usage", Text.placeholder("usage",
                            "/seperm user " + action + " " + target.getName() + " <node>"));
                }
                String node = args[3];
                List<String> nodes = new ArrayList<>(store.get().getStringList(base + ".permissions"));
                if (action.equals("add")) {
                    if (nodes.contains(node)) {
                        throw abort("perm-node-exists", Text.placeholder("node", node));
                    }
                    nodes.add(node);
                } else if (!nodes.remove(node)) {
                    throw abort("perm-node-missing", Text.placeholder("node", node));
                }
                store.get().set(base + ".permissions", nodes);
                refresh(target);
                save(sender, action.equals("add") ? "perm-user-node-added" : "perm-user-node-removed",
                        Text.placeholder("node", node), Text.placeholder("player", target.getName()));
            }
            default -> throw abort("usage", Text.placeholder("usage",
                    "/seperm user <setgroup|add|remove|info> <player> [value]"));
        }
    }

    /**
     * Re-applies permissions for everyone online.
     *
     * <p>Editing one group can change what members of a different group end up with, because of
     * inheritance, so there is no cheaper subset to refresh than "everybody".
     */
    private void refreshAllOnline() {
        if (!isEnabled()) {
            return;
        }
        Bukkit.getOnlinePlayers().forEach(this::apply);
    }

    private void refresh(OfflinePlayer target) {
        Player online = target.getPlayer();
        if (online != null && isEnabled()) {
            apply(online);
        }
    }

    private void requireGroup(String group) {
        if (!store.get().contains("groups." + group)) {
            throw abort("perm-group-unknown", Text.placeholder("group", group));
        }
    }

    private void save(CommandSender sender, String messageKey,
                      net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
        store.save();
        plugin.messages().send(sender, messageKey, resolvers);
    }

    private CommandException abort(String key,
                                   net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
        return new CommandException(plugin.messages().raw(key, resolvers));
    }
}
