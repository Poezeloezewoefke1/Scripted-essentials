package dev.scripted.essentials.features.player;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Effects;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /pauseeffects} — holds the player's current potion effects at their remaining duration.
 *
 * <p>Effects are re-applied with an endless duration while paused, and restored to the exact
 * time they had left when unpaused. Useful when recording, so a buff does not run out mid-take.
 */
public final class PauseEffectsFeature extends Feature {

    private final Map<UUID, List<PotionEffect>> paused = new HashMap<>();

    public PauseEffectsFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("pauseeffects")
                .name("Pause Effects")
                .description("Freezes your potion effect timers, then restores the time left when resumed.")
                .icon(Material.CLOCK)
                .category(FeatureCategory.PLAYER)
                .controls("/pauseeffects")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "pauseeffects", "holdeffects") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                if (paused.containsKey(player.getUniqueId())) {
                    resume(player);
                    plugin.messages().send(sender, "effects-resumed");
                } else {
                    pause(player);
                    plugin.messages().send(sender, "effects-paused");
                }
            }
        }.playerOnly().describe("Freeze your potion effect timers.", "/pauseeffects"));

        listener(new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                // Restore real durations on the way out, so nothing is left endless.
                if (paused.containsKey(event.getPlayer().getUniqueId())) {
                    resume(event.getPlayer());
                }
            }
        });
    }

    @Override
    protected void onDisable() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (paused.containsKey(player.getUniqueId())) {
                resume(player);
            }
        }
        paused.clear();
    }

    private void pause(Player player) {
        List<PotionEffect> snapshot = new ArrayList<>(player.getActivePotionEffects());
        paused.put(player.getUniqueId(), snapshot);
        for (PotionEffect effect : snapshot) {
            player.removePotionEffect(effect.getType());
            player.addPotionEffect(new PotionEffect(effect.getType(), Effects.INFINITE,
                    effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
        }
    }

    private void resume(Player player) {
        List<PotionEffect> snapshot = paused.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        for (PotionEffect effect : snapshot) {
            player.removePotionEffect(effect.getType());
            player.addPotionEffect(effect);
        }
    }
}
