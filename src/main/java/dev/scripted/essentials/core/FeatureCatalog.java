package dev.scripted.essentials.core;

import dev.scripted.essentials.ScriptedEssentials;

/**
 * The single list of every feature the plugin ships.
 *
 * <p>Adding a feature means writing the class and adding one line here; nothing else in the plugin
 * needs to know about it.
 */
public final class FeatureCatalog {

    private FeatureCatalog() {
    }

    public static void populate(ScriptedEssentials plugin, FeatureManager manager) {
        // Populated as features are implemented.
    }
}
