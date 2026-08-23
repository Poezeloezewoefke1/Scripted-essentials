package dev.scripted.essentials.features.chat;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import org.bukkit.Material;

/**
 * The switch behind every interface sound the plugin plays.
 *
 * <p>It has no commands and no listeners of its own: {@link dev.scripted.essentials.util.Sounds}
 * checks this feature's state before playing anything, so switching it off silences the whole
 * plugin in one place.
 */
public final class FeedbackSoundsFeature extends Feature {

    public FeedbackSoundsFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("feedbacksounds")
                .name("Feedback Sounds")
                .description("Plays a short sound when a menu button or command succeeds.")
                .icon(Material.NOTE_BLOCK)
                .category(FeatureCategory.CHAT)
                .build());
    }
}
