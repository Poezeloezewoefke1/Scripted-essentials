package dev.scripted.essentials.features.systems;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Numbers;
import dev.scripted.essentials.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /orbit} — a telegraphed strike on a player or on the block you are looking at.
 *
 * <p>The countdown is deliberately loud: the target is warned and the impact point is marked with
 * particles, so this reads as an event rather than an instant kill. Terrain damage is off by
 * default, which keeps it usable on a survival map.
 */
public final class OrbitalWeaponsFeature extends Feature {

    private static final int COUNTDOWN_SECONDS = 5;

    private final Map<UUID, Long> lastUsed = new HashMap<>();
    private final Map<UUID, BukkitTask> running = new HashMap<>();

    public OrbitalWeaponsFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("orbitalweapons")
                .name("Orbital Weapons")
                .description("Calls a telegraphed lightning strike on a target, with a countdown.")
                .icon(Material.FIRE_CHARGE)
                .category(FeatureCategory.SYSTEMS)
                .controls("/orbit")
                .disabledByDefault()
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "orbit", "orbitalstrike") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player caller = asPlayer(sender);

                long remaining = cooldownRemaining(caller);
                if (remaining > 0) {
                    throw fail("orbit-cooldown",
                            Text.placeholder("time", Numbers.formatDuration(remaining)));
                }

                Location impact;
                Player target = null;
                if (args.length > 0) {
                    target = requireOnline(args[0]);
                    impact = target.getLocation();
                } else {
                    Block looking = caller.getTargetBlockExact(120);
                    if (looking == null) {
                        throw fail("orbit-no-target");
                    }
                    impact = looking.getLocation().add(0.5, 1, 0.5);
                }

                lastUsed.put(caller.getUniqueId(), System.currentTimeMillis());
                begin(caller, target, impact);
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.playerOnly().describe("Call an orbital strike.", "/orbit [player]"));

        listener(new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                BukkitTask task = running.remove(event.getPlayer().getUniqueId());
                if (task != null) {
                    task.cancel();
                }
                lastUsed.remove(event.getPlayer().getUniqueId());
            }
        });
    }

    @Override
    protected void onDisable() {
        running.values().forEach(BukkitTask::cancel);
        running.clear();
        lastUsed.clear();
    }

    /** Counts down, marking the impact point, then fires. */
    private void begin(Player caller, Player target, Location impact) {
        if (target != null) {
            plugin.messages().send(target, "orbit-incoming");
        }
        plugin.messages().send(caller, "orbit-called",
                Text.placeholder("seconds", String.valueOf(COUNTDOWN_SECONDS)));

        BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            private int secondsLeft = COUNTDOWN_SECONDS;

            @Override
            public void run() {
                // Follow a moving target, so dodging means actually running away.
                Location where = target != null && target.isValid() ? target.getLocation() : impact;
                where.getWorld().spawnParticle(Particle.END_ROD, where.clone().add(0, 1, 0), 40,
                        0.4, 2.0, 0.4, 0.01);

                if (secondsLeft-- > 0) {
                    if (target != null) {
                        plugin.messages().send(target, "orbit-countdown",
                                Text.placeholder("seconds", String.valueOf(secondsLeft + 1)));
                    }
                    return;
                }
                strike(where);
                running.remove(caller.getUniqueId());
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);

        running.put(caller.getUniqueId(), task);
    }

    private void strike(Location where) {
        int radius = Math.max(1, plugin.config().getInt("orbital-weapons.radius", 4));
        boolean breakBlocks = plugin.config().getBoolean("orbital-weapons.break-blocks", false);

        for (int offset = -radius; offset <= radius; offset += Math.max(1, radius)) {
            where.getWorld().strikeLightning(where.clone().add(offset, 0, 0));
            where.getWorld().strikeLightning(where.clone().add(0, 0, offset));
        }
        where.getWorld().createExplosion(where, radius, false, breakBlocks);
    }

    private long cooldownRemaining(Player player) {
        if (player.hasPermission("scriptedessentials.orbitalweapons.nocooldown")) {
            return 0;
        }
        int cooldown = plugin.config().getInt("orbital-weapons.cooldown-seconds", 60);
        Long last = lastUsed.get(player.getUniqueId());
        if (last == null || cooldown <= 0) {
            return 0;
        }
        long elapsed = (System.currentTimeMillis() - last) / 1000L;
        return Math.max(0, cooldown - elapsed);
    }
}
