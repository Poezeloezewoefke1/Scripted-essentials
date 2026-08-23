package dev.scripted.essentials.features.teleport;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Locations;
import dev.scripted.essentials.util.Sounds;
import dev.scripted.essentials.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /offlinetp} — teleports to where a player was standing when they logged off.
 *
 * <p>The position is recorded on quit, so it works for players who are no longer on the server.
 */
public final class OfflineTeleportFeature extends Feature {

    private DataFile store;

    public OfflineTeleportFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("offlinetp")
                .name("Offline Teleport")
                .description("Teleports you to the last place an offline player logged out.")
                .icon(Material.ENDER_PEARL)
                .category(FeatureCategory.TELEPORTATION)
                .controls("/offlinetp")
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/last-locations.yml");

        command(new SECommand(plugin, "offlinetp", "otp") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/offlinetp <player>"));
                }
                String key = findEntry(args[0]);
                if (key == null) {
                    throw fail("offlinetp-unknown", Text.placeholder("player", args[0]));
                }
                Location location = Locations.read(store.get(), key + ".location");
                if (location == null) {
                    throw fail("offlinetp-world-missing", Text.placeholder("player", args[0]));
                }

                player.teleport(location);
                Sounds.play(plugin, player, Sounds.TELEPORT, 0.5f, 1.0f);
                plugin.messages().send(sender, "offlinetp-done",
                        Text.placeholder("player", store.get().getString(key + ".name", args[0])),
                        Text.placeholder("location", Locations.describe(location)));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], knownNames()) : List.of();
            }
        }.playerOnly().describe("Teleport to an offline player's last position.", "/offlinetp <player>"));

        listener(new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                if (!isEnabled()) {
                    return;
                }
                Player player = event.getPlayer();
                String key = player.getUniqueId().toString();
                store.get().set(key + ".name", player.getName());
                Locations.write(store.get(), key + ".location", player.getLocation());
                store.save();
            }
        });
    }

    private List<String> knownNames() {
        List<String> names = new ArrayList<>();
        for (String key : store.get().getKeys()) {
            String name = store.get().getString(key + ".name");
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    /** Finds the stored UUID key for a player name, matching case-insensitively. */
    private String findEntry(String name) {
        for (String key : store.get().getKeys()) {
            if (name.equalsIgnoreCase(store.get().getString(key + ".name"))) {
                return key;
            }
        }
        return null;
    }
}
