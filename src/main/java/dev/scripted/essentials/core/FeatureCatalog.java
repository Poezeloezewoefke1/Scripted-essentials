package dev.scripted.essentials.core;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.features.inventory.AutoClearFeature;
import dev.scripted.essentials.features.inventory.EnderChestViewFeature;
import dev.scripted.essentials.features.inventory.InventoryRollbackFeature;
import dev.scripted.essentials.features.inventory.InvseeFeature;
import dev.scripted.essentials.features.inventory.ItemMakerFeature;
import dev.scripted.essentials.features.inventory.KeepInventoryFeature;
import dev.scripted.essentials.features.inventory.KitFeature;
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
import dev.scripted.essentials.features.teleport.OfflineTeleportFeature;
import dev.scripted.essentials.features.teleport.SpawnFeature;
import dev.scripted.essentials.features.teleport.TopFeature;
import dev.scripted.essentials.features.teleport.WarpFeature;

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

        // --- teleportation ---
        manager.register(new TopFeature(plugin));
        manager.register(new OfflineTeleportFeature(plugin));
        manager.register(new SpawnFeature(plugin));
        manager.register(new WarpFeature(plugin));

        // --- inventory ---
        manager.register(new InvseeFeature(plugin));
        manager.register(new EnderChestViewFeature(plugin));
        manager.register(new KeepInventoryFeature(plugin));
        manager.register(new InventoryRollbackFeature(plugin));
        manager.register(new KitFeature(plugin));
        manager.register(new ItemMakerFeature(plugin));
        manager.register(new AutoClearFeature(plugin));
    }
}
