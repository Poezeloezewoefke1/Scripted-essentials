package dev.scripted.essentials.util;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.core.Feature;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Plays interface sounds by their vanilla key.
 *
 * <p>Sounds are addressed as strings rather than through {@code org.bukkit.Sound} on purpose:
 * the Sound type changed shape between Minecraft releases, while the key form of
 * {@code playSound} has been stable for years and also lets server owners name any sound in
 * config.yml, including resource pack sounds.
 */
public final class Sounds {

    public static final String CLICK = "ui.button.click";
    public static final String SUCCESS = "entity.experience_orb.pickup";
    public static final String ERROR = "block.note_block.didgeridoo";
    public static final String TOGGLE_ON = "block.note_block.pling";
    public static final String TOGGLE_OFF = "block.note_block.bass";
    public static final String TELEPORT = "entity.enderman.teleport";
    public static final String EQUIP = "item.armor.equip_generic";
    public static final String LEVEL_UP = "entity.player.levelup";
    public static final String ALERT = "block.anvil.land";

    private Sounds() {
    }

    /** Plays a sound, unless the server has switched the Feedback Sounds feature off. */
    public static void play(ScriptedEssentials plugin, Player player, String key, float volume, float pitch) {
        if (player == null || key == null || key.isBlank() || !feedbackEnabled(plugin)) {
            return;
        }
        player.playSound(player.getLocation(), normalise(key), volume, pitch);
    }

    public static void click(ScriptedEssentials plugin, Player player) {
        play(plugin, player, CLICK, 0.6f, 1.2f);
    }

    public static void success(ScriptedEssentials plugin, Player player) {
        play(plugin, player, SUCCESS, 0.7f, 1.4f);
    }

    public static void error(ScriptedEssentials plugin, Player player) {
        play(plugin, player, ERROR, 0.7f, 0.8f);
    }

    /** Accepts both {@code ENTITY_PLAYER_LEVELUP} and {@code entity.player.levelup} spellings. */
    public static String normalise(String key) {
        return key.trim().toLowerCase(Locale.ROOT).replace('_', '.');
    }

    private static boolean feedbackEnabled(ScriptedEssentials plugin) {
        return plugin.features().get("feedbacksounds").map(Feature::isEnabled).orElse(true);
    }
}
