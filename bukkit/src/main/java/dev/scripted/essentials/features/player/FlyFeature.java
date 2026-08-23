package dev.scripted.essentials.features.player;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Numbers;
import dev.scripted.essentials.util.Text;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /fly} — flight toggle with a fly speed control.
 *
 * <p>Turning flight off in mid-air would normally kill the player on landing, so the next fall
 * they take is absorbed.
 */
public final class FlyFeature extends Feature {

    private final Set<UUID> fallGrace = new HashSet<>();

    public FlyFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("fly")
                .name("Fly")
                .description("Toggles flight, with fall damage protection on landing and a speed control.")
                .icon(Material.FEATHER)
                .category(FeatureCategory.PLAYER)
                .controls("/fly", "/flyspeed")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "fly") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player target = resolveTarget(sender, args, 0);
                boolean enable = !target.getAllowFlight();
                setFlight(target, enable);

                if (sender == target) {
                    plugin.messages().send(sender, enable ? "fly-on-self" : "fly-off-self");
                } else {
                    plugin.messages().send(sender, enable ? "fly-on-other" : "fly-off-other",
                            Text.placeholder("player", target.getName()));
                    plugin.messages().send(target, enable ? "fly-on-self" : "fly-off-self");
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
            }
        }.describe("Toggle flight.", "/fly [player]"));

        command(new SECommand(plugin, "flyspeed") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    throw fail("usage", Text.placeholder("usage", "/flyspeed <1-10> [player]"));
                }
                double speed = Numbers.parseDouble(args[0])
                        .orElseThrow(() -> fail("invalid-number", Text.placeholder("input", args[0])));
                Player target = resolveTarget(sender, args, 1);

                // Bukkit takes -1..1; players think in 1..10, where 1 is the vanilla speed.
                float scaled = (float) (Numbers.clamp(speed, 1, 10) / 10.0);
                target.setFlySpeed(scaled);
                plugin.messages().send(sender, "flyspeed-set",
                        Text.placeholder("player", target.getName()),
                        Text.placeholder("value", String.valueOf(Numbers.clamp(speed, 1, 10))));
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(args[0], List.of("1", "2", "5", "10"));
                }
                return args.length == 2 ? onlineNames(sender, args[1]) : List.of();
            }
        }.describe("Set a player's flight speed.", "/flyspeed <1-10> [player]"));

        listener(new Listener() {
            @EventHandler(ignoreCancelled = true)
            public void onDamage(EntityDamageEvent event) {
                if (!isEnabled() || event.getCause() != EntityDamageEvent.DamageCause.FALL) {
                    return;
                }
                if (event.getEntity() instanceof Player player
                        && fallGrace.remove(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                fallGrace.remove(event.getPlayer().getUniqueId());
            }
        });
    }

    @Override
    protected void onDisable() {
        // Leave nobody stuck in creative flight when the feature is switched off.
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getAllowFlight() && player.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                setFlight(player, false);
            }
        }
        fallGrace.clear();
    }

    private void setFlight(Player player, boolean enable) {
        player.setAllowFlight(enable);
        player.setFlying(enable);
        if (!enable) {
            fallGrace.add(player.getUniqueId());
        } else {
            fallGrace.remove(player.getUniqueId());
        }
    }
}
