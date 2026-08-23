package dev.scripted.essentials.features.moderation;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Sounds;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Stops any hit from killing a player outright.
 *
 * <p>A blow that would take someone below the configured floor is cancelled and their health is
 * pinned there instead, so they always get a chance to react. Cancelling rather than reducing the
 * damage is deliberate: {@code getFinalDamage} already includes armour and resistance, and there
 * is no way to write a base damage value back that lands on an exact resulting health.
 */
public final class HalfHeartFeature extends Feature {

    private static final Set<EntityDamageEvent.DamageCause> UNSTOPPABLE = EnumSet.of(
            EntityDamageEvent.DamageCause.VOID,
            EntityDamageEvent.DamageCause.SUICIDE,
            EntityDamageEvent.DamageCause.KILL);

    public HalfHeartFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("halfheart")
                .name("Half Heart")
                .description("No single hit can kill: damage is capped at half a heart of health.")
                .icon(Material.REDSTONE)
                .category(FeatureCategory.MODERATION)
                .controls("/halfheart")
                .disabledByDefault()
                .build());
    }

    @Override
    protected void onRegister() {
        commandUnbound(new SECommand(plugin, "halfheart") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                boolean target = args.length > 0
                        ? args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("true")
                        : !isEnabled();
                setFeatureEnabled(target);
                plugin.messages().send(sender, target ? "halfheart-on" : "halfheart-off");
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(args[0], List.of("on", "off")) : List.of();
            }
        }.permission(definition().permission())
                .describe("Toggle half heart protection.", "/halfheart [on|off]"));

        listener(new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onDamage(EntityDamageEvent event) {
                if (!isEnabled() || !(event.getEntity() instanceof Player player)) {
                    return;
                }
                if (player.hasPermission("scriptedessentials.halfheart.exempt")) {
                    return;
                }
                // Surviving these would be worse than dying: a capped void hit leaves the player
                // falling forever at half a heart, and capping /kill makes it look broken.
                if (UNSTOPPABLE.contains(event.getCause())) {
                    return;
                }
                double floor = plugin.config().getDouble("half-heart.minimum-health", 1.0);
                double remaining = player.getHealth() - event.getFinalDamage();
                if (remaining >= floor) {
                    return;
                }

                event.setCancelled(true);
                player.setHealth(Math.min(floor, player.getHealth()));
                player.setFireTicks(0);

                String sound = plugin.config().getString("half-heart.sound", "");
                if (!sound.isBlank()) {
                    Sounds.play(plugin, player, sound, 0.7f, 1.6f);
                }
            }
        });
    }
}
