package dev.scripted.essentials.features.inventory;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Items;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Keeps recent copies of every player's inventory so a lost one can be handed back.
 *
 * <p>Snapshots are taken on death and on quit, and the oldest are pruned once the per-player
 * limit is reached. {@code /rollback <player>} opens the list; clicking an entry restores it.
 */
public final class InventoryRollbackFeature extends Feature {

    private DataFile store;

    public InventoryRollbackFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("inventoryrollback")
                .name("Inventory Rollback")
                .description("Saves inventory snapshots on death and quit, restorable from a menu.")
                .icon(Material.WRITABLE_BOOK)
                .category(FeatureCategory.INVENTORY)
                .controls("/rollback")
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/rollbacks.yml");

        command(new SECommand(plugin, "rollback", "invrollback") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player viewer = asPlayer(sender);
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/rollback <player>"));
                }
                UUID id = resolveId(args[0]);
                if (id == null || snapshots(id).isEmpty()) {
                    throw fail("rollback-none", Text.placeholder("player", args[0]));
                }
                new RollbackMenu(plugin, viewer, InventoryRollbackFeature.this, id, args[0]).open();
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], knownNames()) : List.of();
            }
        }.playerOnly().describe("Browse a player's inventory snapshots.", "/rollback <player>"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onDeath(PlayerDeathEvent event) {
                if (isEnabled() && plugin.getConfig().getBoolean("inventory-rollback.snapshot-on-death", true)) {
                    capture(event.getEntity(), "death");
                }
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                if (isEnabled() && plugin.getConfig().getBoolean("inventory-rollback.snapshot-on-quit", true)) {
                    capture(event.getPlayer(), "quit");
                }
            }
        });
    }

    /** Stores the player's current inventory under the given reason. */
    public void capture(Player player, String reason) {
        String base = player.getUniqueId() + ".snapshots." + System.currentTimeMillis();
        store.get().set(player.getUniqueId() + ".name", player.getName());
        store.get().set(base + ".reason", reason);
        Items.writeArray(store.get(), base + ".contents", player.getInventory().getContents());
        Items.writeArray(store.get(), base + ".armor", player.getInventory().getArmorContents());

        prune(player.getUniqueId());
        store.save();
    }

    /** Snapshot timestamps for a player, newest first. */
    public List<Long> snapshots(UUID id) {
        ConfigurationSection section = store.get().getConfigurationSection(id + ".snapshots");
        if (section == null) {
            return List.of();
        }
        List<Long> stamps = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            try {
                stamps.add(Long.parseLong(key));
            } catch (NumberFormatException ignored) {
                // A hand-edited key that is not a timestamp; skip it rather than fail the menu.
            }
        }
        stamps.sort(Comparator.reverseOrder());
        return stamps;
    }

    public String reasonOf(UUID id, long stamp) {
        return store.get().getString(id + ".snapshots." + stamp + ".reason", "unknown");
    }

    public int itemCountOf(UUID id, long stamp) {
        return Items.countNonEmpty(Items.readArray(store.get(), id + ".snapshots." + stamp + ".contents", 41));
    }

    /** Puts a snapshot back on a player. Anything that will not fit is dropped at their feet. */
    public boolean restore(UUID id, long stamp, Player target) {
        String base = id + ".snapshots." + stamp;
        if (!store.get().contains(base)) {
            return false;
        }
        ItemStack[] contents = Items.readArray(store.get(), base + ".contents",
                target.getInventory().getContents().length);
        ItemStack[] armor = Items.readArray(store.get(), base + ".armor", 4);

        // Snapshot what they have now first, so a mistaken restore is itself undoable.
        capture(target, "pre-restore");

        target.getInventory().setContents(contents);
        target.getInventory().setArmorContents(armor);
        target.updateInventory();
        return true;
    }

    private void prune(UUID id) {
        int limit = Math.max(1, plugin.getConfig().getInt("inventory-rollback.snapshots-per-player", 10));
        List<Long> stamps = snapshots(id);
        for (int index = limit; index < stamps.size(); index++) {
            store.get().set(id + ".snapshots." + stamps.get(index), null);
        }
    }

    private List<String> knownNames() {
        List<String> names = new ArrayList<>();
        for (String key : store.get().getKeys(false)) {
            String name = store.get().getString(key + ".name");
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    private UUID resolveId(String name) {
        for (String key : store.get().getKeys(false)) {
            if (name.equalsIgnoreCase(store.get().getString(key + ".name"))) {
                try {
                    return UUID.fromString(key);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
    }
}
