package dev.scripted.essentials.features.npc;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Locations;
import dev.scripted.essentials.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Clickable, scripted NPCs built from ordinary entities.
 *
 * <p>No packets and no Citizens dependency: an NPC is a real entity with its AI, gravity and
 * damage switched off, tagged in its persistent data container so the plugin recognises it again
 * after a restart. That means NPCs cannot wear another player's skin — that genuinely needs
 * packet-level access — but everything else (naming, clicking, running actions) works on a plain
 * Paper server.
 */
public final class NpcFeature extends Feature {

    private final Map<UUID, Long> lastInteract = new HashMap<>();
    private NamespacedKey npcKey;
    private DataFile store;

    public NpcFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("npcs")
                .name("Custom NPCs")
                .description("Clickable NPCs that run commands, send messages or open menus.")
                .icon(Material.VILLAGER_SPAWN_EGG)
                .category(FeatureCategory.SYSTEMS)
                .controls("/npc")
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/npcs.yml");
        this.npcKey = new NamespacedKey(plugin, "npc_id");

        command(new SECommand(plugin, "npc") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage",
                            "/npc <create|remove|list|action|tp> ..."));
                }
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "create" -> create(sender, args);
                    case "remove" -> remove(sender, args);
                    case "list" -> list(sender);
                    case "action" -> action(sender, args);
                    case "tp" -> teleportTo(sender, args);
                    default -> throw fail("usage", Text.placeholder("usage",
                            "/npc <create|remove|list|action|tp> ..."));
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(args[0], List.of("create", "remove", "list", "action", "tp"));
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
                    List<String> types = new ArrayList<>();
                    for (EntityType type : EntityType.values()) {
                        types.add(type.name().toLowerCase(Locale.ROOT));
                    }
                    return filter(args[1], types);
                }
                if (args.length == 2) {
                    return filter(args[1], npcIds());
                }
                if (args.length == 3 && args[0].equalsIgnoreCase("action")) {
                    return filter(args[2], List.of("add", "clear", "list"));
                }
                if (args.length == 4 && args[0].equalsIgnoreCase("action")
                        && args[2].equalsIgnoreCase("add")) {
                    return filter(args[3], List.of("command", "console", "message", "warp", "kit"));
                }
                return List.of();
            }
        }.describe("Create and manage NPCs.", "/npc <create|remove|list|action|tp>"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
            public void onInteract(PlayerInteractEntityEvent event) {
                if (!isEnabled()) {
                    return;
                }
                String id = idOf(event.getRightClicked());
                if (id == null) {
                    return;
                }
                event.setCancelled(true);
                if (onCooldown(event.getPlayer())) {
                    return;
                }
                runActions(event.getPlayer(), id);
            }

            @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
            public void onDamage(EntityDamageEvent event) {
                if (isEnabled() && idOf(event.getEntity()) != null) {
                    event.setCancelled(true);
                }
            }
        });
    }

    // ---- subcommands ------------------------------------------------------------------------

    private void create(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (args.length < 3) {
            throw abort("usage", Text.placeholder("usage", "/npc create <type> <id> [display name]"));
        }
        EntityType type;
        try {
            type = EntityType.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw abort("npc-unknown-type", Text.placeholder("input", args[1]));
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        if (store.get().contains("npcs." + id)) {
            throw abort("npc-exists", Text.placeholder("id", id));
        }
        String displayName = args.length > 3
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length))
                : id;

        Entity entity = player.getWorld().spawnEntity(player.getLocation(), type);
        applyNpcTraits(entity, displayName, id);

        store.get().set("npcs." + id + ".entity", entity.getUniqueId().toString());
        store.get().set("npcs." + id + ".type", type.name());
        store.get().set("npcs." + id + ".name", displayName);
        store.get().set("npcs." + id + ".actions", new ArrayList<String>());
        Locations.write(store.get(), "npcs." + id + ".location", player.getLocation());
        store.save();

        plugin.messages().send(sender, "npc-created", Text.placeholder("id", id));
    }

    private void remove(CommandSender sender, String[] args) {
        String id = requireId(args, 1);
        Entity entity = entityOf(id);
        if (entity != null) {
            entity.remove();
        }
        store.get().set("npcs." + id, null);
        store.save();
        plugin.messages().send(sender, "npc-removed", Text.placeholder("id", id));
    }

    private void list(CommandSender sender) {
        List<String> ids = npcIds();
        plugin.messages().send(sender, "npc-list", Text.placeholder("npcs",
                ids.isEmpty() ? "none" : String.join(", ", ids)));
    }

    private void action(CommandSender sender, String[] args) {
        String id = requireId(args, 1);
        if (args.length < 3) {
            throw abort("usage", Text.placeholder("usage",
                    "/npc action <id> <add|clear|list> [type] [value]"));
        }
        List<String> actions = new ArrayList<>(store.get().getStringList("npcs." + id + ".actions"));

        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "clear" -> {
                store.get().set("npcs." + id + ".actions", new ArrayList<String>());
                store.save();
                plugin.messages().send(sender, "npc-actions-cleared", Text.placeholder("id", id));
            }
            case "list" -> plugin.messages().send(sender, "npc-actions-list",
                    Text.placeholder("id", id),
                    Text.placeholder("actions", actions.isEmpty() ? "none" : String.join(" | ", actions)));
            case "add" -> {
                if (args.length < 5) {
                    throw abort("usage", Text.placeholder("usage",
                            "/npc action <id> add <command|console|message|warp|kit> <value>"));
                }
                String type = args[3].toLowerCase(Locale.ROOT);
                String value = String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length));
                actions.add(type + ":" + value);
                store.get().set("npcs." + id + ".actions", actions);
                store.save();
                plugin.messages().send(sender, "npc-action-added",
                        Text.placeholder("id", id), Text.placeholder("action", type + ":" + value));
            }
            default -> throw abort("usage", Text.placeholder("usage",
                    "/npc action <id> <add|clear|list> [type] [value]"));
        }
    }

    private void teleportTo(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        String id = requireId(args, 1);
        Location location = Locations.read(store.get(), "npcs." + id + ".location");
        if (location == null) {
            throw abort("npc-world-missing", Text.placeholder("id", id));
        }
        player.teleport(location);
        plugin.messages().send(sender, "npc-teleported", Text.placeholder("id", id));
    }

    // ---- behaviour --------------------------------------------------------------------------

    /** Turns a freshly spawned entity into a static, clickable NPC. */
    private void applyNpcTraits(Entity entity, String displayName, String id) {
        entity.customName(Text.parse(displayName));
        entity.setCustomNameVisible(true);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.setGravity(false);
        entity.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, id);

        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setCollidable(false);
            living.setRemoveWhenFarAway(false);
            living.setCanPickupItems(false);
        }
    }

    /** Runs every configured action for an NPC, in order. */
    private void runActions(Player player, String id) {
        for (String action : store.get().getStringList("npcs." + id + ".actions")) {
            int split = action.indexOf(':');
            if (split < 0) {
                continue;
            }
            String type = action.substring(0, split).toLowerCase(Locale.ROOT);
            String value = action.substring(split + 1)
                    .replace("<player>", player.getName());

            switch (type) {
                case "command" -> player.performCommand(value);
                case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value);
                case "message" -> player.sendMessage(Text.parse(value));
                case "warp" -> player.performCommand("warp " + value);
                case "kit" -> player.performCommand("kit " + value);
                default -> plugin.getLogger().warning(
                        "NPC '" + id + "' has an unknown action type: " + type);
            }
        }
    }

    /** One click can fire both hands; this collapses that into a single action run. */
    private boolean onCooldown(Player player) {
        long window = plugin.getConfig().getLong("npcs.interact-cooldown-ms", 500L);
        long now = System.currentTimeMillis();
        Long previous = lastInteract.get(player.getUniqueId());
        if (previous != null && now - previous < window) {
            return true;
        }
        lastInteract.put(player.getUniqueId(), now);
        return false;
    }

    private String idOf(Entity entity) {
        return entity.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
    }

    private Entity entityOf(String id) {
        String raw = store.get().getString("npcs." + id + ".entity");
        if (raw == null) {
            return null;
        }
        try {
            return Bukkit.getEntity(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<String> npcIds() {
        ConfigurationSection section = store.get().getConfigurationSection("npcs");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    private String requireId(String[] args, int index) {
        if (args.length <= index) {
            throw abort("usage", Text.placeholder("usage", "/npc " + args[0] + " <id>"));
        }
        String id = args[index].toLowerCase(Locale.ROOT);
        if (!store.get().contains("npcs." + id)) {
            throw abort("npc-unknown", Text.placeholder("id", args[index]));
        }
        return id;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        throw abort("player-only");
    }

    private dev.scripted.essentials.command.CommandException abort(
            String key, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
        return new dev.scripted.essentials.command.CommandException(
                plugin.messages().raw(key, resolvers));
    }
}
