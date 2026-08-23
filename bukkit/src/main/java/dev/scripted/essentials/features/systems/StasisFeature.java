package dev.scripted.essentials.features.systems;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Locations;
import dev.scripted.essentials.util.Numbers;
import dev.scripted.essentials.util.Sounds;
import dev.scripted.essentials.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Holds a thrown ender pearl in mid-air until it is called back.
 *
 * <p>With stasis armed, the next pearl you throw stops where it is instead of landing. Running
 * {@code /stasis release} teleports you to it and consumes it. Pearls expire after a configurable
 * hold time so they cannot be parked on the map forever.
 */
public final class StasisFeature extends Feature {

    private final Set<UUID> armed = new HashSet<>();
    private final Map<UUID, UUID> heldPearls = new HashMap<>();
    private final Map<UUID, Location> heldLocations = new HashMap<>();
    private final Map<UUID, Long> heldSince = new HashMap<>();

    public StasisFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("stasis")
                .name("Stasis Rod")
                .description("Freezes a thrown ender pearl in the air, to be recalled on command.")
                .icon(Material.BLAZE_ROD)
                .category(FeatureCategory.SYSTEMS)
                .controls("/stasis", "/stasis release")
                .disabledByDefault()
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "stasis") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);

                if (args.length > 0 && args[0].equalsIgnoreCase("release")) {
                    release(player);
                    return;
                }
                boolean nowArmed = !armed.remove(player.getUniqueId());
                if (nowArmed) {
                    armed.add(player.getUniqueId());
                }
                plugin.messages().send(sender, nowArmed ? "stasis-armed" : "stasis-disarmed");
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], List.of("release")) : List.of();
            }
        }.playerOnly().describe("Arm stasis, or recall a held pearl.", "/stasis [release]"));

        listener(new Listener() {
            @EventHandler(ignoreCancelled = true)
            public void onLaunch(ProjectileLaunchEvent event) {
                if (!isEnabled() || !(event.getEntity() instanceof EnderPearl pearl)) {
                    return;
                }
                if (!(pearl.getShooter() instanceof Player thrower)
                        || !armed.remove(thrower.getUniqueId())) {
                    return;
                }
                // Let the pearl leave the hand first, then pin it where it got to.
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> hold(thrower, pearl), 2L);
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                forget(event.getPlayer().getUniqueId());
            }
        });
    }

    @Override
    protected void onDisable() {
        for (UUID owner : Set.copyOf(heldPearls.keySet())) {
            forget(owner);
        }
        armed.clear();
    }

    private void hold(Player thrower, EnderPearl pearl) {
        if (!pearl.isValid()) {
            return;
        }
        pearl.setGravity(false);
        pearl.setVelocity(new Vector(0, 0, 0));

        UUID owner = thrower.getUniqueId();
        forget(owner);
        heldPearls.put(owner, pearl.getUniqueId());
        heldLocations.put(owner, pearl.getLocation());
        heldSince.put(owner, System.currentTimeMillis());

        plugin.messages().send(thrower, "stasis-held",
                Text.placeholder("location", Locations.describe(pearl.getLocation())));

        int maxHold = plugin.config().getInt("stasis.max-hold-seconds", 600);
        if (maxHold > 0) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (pearl.getUniqueId().equals(heldPearls.get(owner))) {
                    forget(owner);
                    Player online = plugin.getServer().getPlayerExact(thrower.getName());
                    if (online != null) {
                        plugin.messages().send(online, "stasis-expired");
                    }
                }
            }, maxHold * 20L);
        }
    }

    private void release(Player player) {
        Location destination = heldLocations.get(player.getUniqueId());
        if (destination == null) {
            throw new dev.scripted.essentials.command.CommandException(
                    plugin.messages().raw("stasis-nothing-held"));
        }
        long heldFor = (System.currentTimeMillis()
                - heldSince.getOrDefault(player.getUniqueId(), System.currentTimeMillis())) / 1000L;

        forget(player.getUniqueId());
        player.teleport(destination);
        Sounds.play(plugin, player, Sounds.TELEPORT, 0.6f, 1.0f);

        plugin.messages().send(player, "stasis-released",
                Text.placeholder("time", Numbers.formatDuration(heldFor)));
    }

    /** Drops the record for a player and removes the pearl entity if it is still around. */
    private void forget(UUID owner) {
        UUID pearlId = heldPearls.remove(owner);
        heldLocations.remove(owner);
        heldSince.remove(owner);
        if (pearlId == null) {
            return;
        }
        org.bukkit.entity.Entity pearl = org.bukkit.Bukkit.getEntity(pearlId);
        if (pearl != null) {
            pearl.remove();
        }
    }
}
