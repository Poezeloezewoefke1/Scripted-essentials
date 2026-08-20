package dev.scripted.essentials.features.npc;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.CommandException;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Locations;
import dev.scripted.essentials.util.Numbers;
import dev.scripted.essentials.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Decorative villagers for shops and hubs.
 *
 * <p>These are set to a fixed profession and level with their AI off and their trade window
 * blocked, so they stay where they are put, never restock, and cannot be traded with by accident.
 */
public final class VillagerMakerFeature extends Feature {

    private static final List<String> COMMON_PROFESSIONS = List.of(
            "armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher",
            "leatherworker", "librarian", "mason", "nitwit", "none", "shepherd", "toolsmith", "weaponsmith");

    private NamespacedKey villagerKey;
    private DataFile store;

    public VillagerMakerFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("villagermaker")
                .name("Custom Villager Maker")
                .description("Places decorative villagers with a fixed profession and no trading.")
                .icon(Material.EMERALD)
                .category(FeatureCategory.SYSTEMS)
                .controls("/villagermaker")
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/villagers.yml");
        this.villagerKey = new NamespacedKey(plugin, "villager_id");

        command(new SECommand(plugin, "villagermaker", "vm") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage",
                            "/villagermaker <create|remove|list> ..."));
                }
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "create" -> create(sender, args);
                    case "remove" -> remove(sender, args);
                    case "list" -> plugin.messages().send(sender, "villager-list",
                            Text.placeholder("villagers", ids().isEmpty() ? "none" : String.join(", ", ids())));
                    default -> throw fail("usage", Text.placeholder("usage",
                            "/villagermaker <create|remove|list> ..."));
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(args[0], List.of("create", "remove", "list"));
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
                    return filter(args[1], COMMON_PROFESSIONS);
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                    return filter(args[1], ids());
                }
                return List.of();
            }
        }.describe("Create decorative villagers.", "/villagermaker <create|remove|list>"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
            public void onInteract(PlayerInteractEntityEvent event) {
                // Block the trade window; the NPC feature still handles its own click actions.
                if (isEnabled() && event.getRightClicked().getPersistentDataContainer()
                        .has(villagerKey, PersistentDataType.STRING)) {
                    event.setCancelled(true);
                }
            }
        });
    }

    private void create(CommandSender sender, String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            throw new CommandException(plugin.messages().raw("player-only"));
        }
        if (args.length < 3) {
            throw new CommandException(plugin.messages().raw("usage", Text.placeholder("usage",
                    "/villagermaker create <profession> <id> [level] [display name]")));
        }
        Villager.Profession profession = professionOf(args[1]);
        if (profession == null) {
            throw new CommandException(plugin.messages().raw("villager-unknown-profession",
                    Text.placeholder("input", args[1])));
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        if (store.get().contains("villagers." + id)) {
            throw new CommandException(plugin.messages().raw("villager-exists",
                    Text.placeholder("id", id)));
        }
        int level = args.length > 3 ? Numbers.parseInt(args[3]).orElse(1) : 1;
        String displayName = args.length > 4
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length))
                : id;

        Entity spawned = player.getWorld().spawnEntity(player.getLocation(), EntityType.VILLAGER);
        if (!(spawned instanceof Villager villager)) {
            spawned.remove();
            throw new CommandException(plugin.messages().raw("villager-spawn-failed"));
        }
        villager.setProfession(profession);
        villager.setVillagerLevel(Numbers.clamp(level, 1, 5));
        villager.customName(Text.parse(displayName));
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setCollidable(false);
        villager.setRemoveWhenFarAway(false);
        villager.getPersistentDataContainer().set(villagerKey, PersistentDataType.STRING, id);

        store.get().set("villagers." + id + ".entity", villager.getUniqueId().toString());
        store.get().set("villagers." + id + ".profession", args[1].toLowerCase(Locale.ROOT));
        store.get().set("villagers." + id + ".name", displayName);
        Locations.write(store.get(), "villagers." + id + ".location", player.getLocation());
        store.save();

        plugin.messages().send(sender, "villager-created", Text.placeholder("id", id));
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            throw new CommandException(plugin.messages().raw("usage",
                    Text.placeholder("usage", "/villagermaker remove <id>")));
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!store.get().contains("villagers." + id)) {
            throw new CommandException(plugin.messages().raw("villager-unknown",
                    Text.placeholder("id", args[1])));
        }
        String raw = store.get().getString("villagers." + id + ".entity");
        if (raw != null) {
            try {
                Entity entity = Bukkit.getEntity(UUID.fromString(raw));
                if (entity != null) {
                    entity.remove();
                }
            } catch (IllegalArgumentException ignored) {
                // Stored id is not a UUID; the config entry still goes.
            }
        }
        store.get().set("villagers." + id, null);
        store.save();
        plugin.messages().send(sender, "villager-removed", Text.placeholder("id", id));
    }

    private Villager.Profession professionOf(String name) {
        try {
            return Registry.VILLAGER_PROFESSION.get(
                    NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<String> ids() {
        ConfigurationSection section = store.get().getConfigurationSection("villagers");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    /** The last known position of a placed villager, for tooling and future menus. */
    public Location locationOf(String id) {
        return Locations.read(store.get(), "villagers." + id + ".location");
    }
}
