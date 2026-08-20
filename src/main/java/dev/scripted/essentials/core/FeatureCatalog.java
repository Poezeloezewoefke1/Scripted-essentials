package dev.scripted.essentials.core;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.features.player.DurabilityFeature;
import dev.scripted.essentials.features.player.FeedFeature;
import dev.scripted.essentials.features.player.FlyFeature;
import dev.scripted.essentials.features.player.FullToolsFeature;
import dev.scripted.essentials.features.player.GameModeFeature;
import dev.scripted.essentials.features.player.HealFeature;
import dev.scripted.essentials.features.player.HungerFeature;
import dev.scripted.essentials.features.player.InvisibilityFeature;
import dev.scripted.essentials.features.player.NoEffectsFeature;
import dev.scripted.essentials.features.player.PauseEffectsFeature;
import dev.scripted.essentials.features.player.PotionPresetsFeature;
import dev.scripted.essentials.features.player.RandomHeadFeature;
import dev.scripted.essentials.features.player.RepairFeature;
import dev.scripted.essentials.features.player.SetHealthFeature;
import dev.scripted.essentials.features.player.TotemFeature;

/**
 * The single list of every feature the plugin ships.
 *
 * <p>Adding a feature means writing the class and adding one line here; nothing else in the
 * plugin needs to know about it. Registration order is the order icons appear in {@code /se}.
 */
public final class FeatureCatalog {

    private FeatureCatalog() {
    }

    public static void populate(ScriptedEssentials plugin, FeatureManager manager) {
        // --- player ---
        manager.register(new HealFeature(plugin));
        manager.register(new FeedFeature(plugin));
        manager.register(new HungerFeature(plugin));
        manager.register(new SetHealthFeature(plugin));
        manager.register(new FlyFeature(plugin));
        manager.register(new GameModeFeature(plugin));
        manager.register(new RepairFeature(plugin));
        manager.register(new DurabilityFeature(plugin));
        manager.register(new FullToolsFeature(plugin));
        manager.register(new InvisibilityFeature(plugin));
        manager.register(new NoEffectsFeature(plugin));
        manager.register(new PauseEffectsFeature(plugin));
        manager.register(new PotionPresetsFeature(plugin));
        manager.register(new TotemFeature(plugin));
        manager.register(new RandomHeadFeature(plugin));
    }
}
