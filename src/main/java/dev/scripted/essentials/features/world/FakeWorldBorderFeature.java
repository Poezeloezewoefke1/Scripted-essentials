package dev.scripted.essentials.features.world;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.storage.DataFile;
import dev.scripted.essentials.util.Numbers;
import dev.scripted.essentials.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * A soft play-area limit that pushes players back instead of using the vanilla border.
 *
 * <p>Unlike the real world border this leaves world generation and the client's border overlay
 * alone, so an event area can be fenced off without the map looking permanently shrunk. The limit
 * is a square measured from the world spawn.
 */
public final class FakeWorldBorderFeature extends Feature {

    private static final String BYPASS = "scriptedessentials.bypass";
    private static final long MESSAGE_COOLDOWN_MS = 3000L;

    private final Map<UUID, Long> lastWarned = new HashMap<>();
    private DataFile store;

    public FakeWorldBorderFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("fakeworldborder")
                .name("Fake World Border")
                .description("Pushes players back at a set distance from spawn, per world.")
                .icon(Material.BLUE_ICE)
                .category(FeatureCategory.WORLD)
                .controls("/fakeborder")
                .disabledByDefault()
                .build());
    }

    @Override
    protected void onRegister() {
        this.store = data("data/borders.yml");

        command(new SECommand(plugin, "fakeborder", "softborder") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                World world = player.getWorld();
                String key = "borders." + world.getName().toLowerCase(Locale.ROOT);

                if (args.length == 0) {
                    plugin.messages().send(sender, "fakeborder-current",
                            Text.placeholder("world", world.getName()),
                            Text.placeholder("radius", String.valueOf(radiusOf(world))));
                    return;
                }
                if (args[0].equalsIgnoreCase("off")) {
                    store.get().set(key, 0);
                    store.save();
                    plugin.messages().send(sender, "fakeborder-off",
                            Text.placeholder("world", world.getName()));
                    return;
                }
                int radius = Numbers.parseInt(args[0])
                        .orElseThrow(() -> fail("invalid-number", Text.placeholder("input", args[0])));
                store.get().set(key, Math.max(1, radius));
                store.save();

                plugin.messages().send(sender, "fakeborder-set",
                        Text.placeholder("world", world.getName()),
                        Text.placeholder("radius", String.valueOf(Math.max(1, radius))));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], List.of("500", "1000", "5000", "off")) : List.of();
            }
        }.playerOnly().describe("Set this world's soft border.", "/fakeborder <radius|off>"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
            public void onMove(PlayerMoveEvent event) {
                if (!isEnabled() || event.getTo() == null) {
                    return;
                }
                Player player = event.getPlayer();
                if (player.hasPermission(BYPASS)) {
                    return;
                }
                // Only check when the player actually changed block, not on every head turn.
                if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                        && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
                    return;
                }
                int radius = radiusOf(player.getWorld());
                if (radius <= 0) {
                    return;
                }
                Location centre = player.getWorld().getSpawnLocation();
                Location to = event.getTo();
                double dx = to.getX() - centre.getX();
                double dz = to.getZ() - centre.getZ();

                if (Math.abs(dx) <= radius && Math.abs(dz) <= radius) {
                    return;
                }
                Location pushed = to.clone();
                pushed.setX(centre.getX() + Numbers.clamp(dx, -radius, radius));
                pushed.setZ(centre.getZ() + Numbers.clamp(dz, -radius, radius));
                event.setTo(pushed);
                warn(player);
            }
        });
    }

    @Override
    protected void onDisable() {
        lastWarned.clear();
    }

    public int radiusOf(World world) {
        String key = "borders." + world.getName().toLowerCase(Locale.ROOT);
        int configured = plugin.getConfig().getInt("fake-world-border.default-radius", 5000);
        return store.get().getInt(key, configured);
    }

    /** Rate limited so walking along the edge does not spam chat. */
    private void warn(Player player) {
        long now = System.currentTimeMillis();
        Long previous = lastWarned.get(player.getUniqueId());
        if (previous != null && now - previous < MESSAGE_COOLDOWN_MS) {
            return;
        }
        lastWarned.put(player.getUniqueId(), now);
        player.sendMessage(Text.parse(plugin.getConfig().getString("fake-world-border.message",
                "<red>You have reached the edge of the world.")));
    }
}
