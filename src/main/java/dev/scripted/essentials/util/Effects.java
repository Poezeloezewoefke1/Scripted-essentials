package dev.scripted.essentials.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

/** Looks potion effect types up by their vanilla key, for the same reason as {@link Enchants}. */
public final class Effects {

    /** Duration value Minecraft treats as "never expires", used by Pause Effects and Recording Mode. */
    public static final int INFINITE = -1;

    private Effects() {
    }

    public static PotionEffectType byKey(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("minecraft:")) {
            key = key.substring("minecraft:".length());
        }
        try {
            return Registry.EFFECT.get(NamespacedKey.minecraft(key));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Parses {@code speed:2:30} (type, amplifier as seen in-game, seconds) into an effect.
     * Amplifier is 1-based here because that is how players read potion levels.
     */
    public static PotionEffect parse(String spec) {
        String[] parts = spec.split(":");
        PotionEffectType type = byKey(parts[0]);
        if (type == null) {
            return null;
        }
        int level = parts.length > 1 ? Numbers.parseInt(parts[1]).orElse(1) : 1;
        int seconds = parts.length > 2 ? Numbers.parseDuration(parts[2]).orElse(60) : 60;
        int duration = seconds <= 0 ? INFINITE : seconds * 20;
        return new PotionEffect(type, duration, Math.max(0, level - 1), false, true, true);
    }
}
