package dev.scripted.essentials.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

import java.util.Locale;

/**
 * Looks enchantments up by their vanilla key.
 *
 * <p>Key lookup rather than {@code Enchantment.SHARPNESS}: the constant names were renamed in
 * 1.20.5 (DAMAGE_ALL became SHARPNESS, DURABILITY became UNBREAKING) while the registry keys
 * have never changed, so this keeps working across releases and lets config files name any
 * enchantment including modded ones.
 */
public final class Enchants {

    public static final String SHARPNESS = "sharpness";
    public static final String EFFICIENCY = "efficiency";
    public static final String UNBREAKING = "unbreaking";
    public static final String FORTUNE = "fortune";
    public static final String MENDING = "mending";
    public static final String PROTECTION = "protection";
    public static final String FEATHER_FALLING = "feather_falling";
    public static final String DEPTH_STRIDER = "depth_strider";
    public static final String LOOTING = "looting";
    public static final String FIRE_ASPECT = "fire_aspect";
    public static final String POWER = "power";
    public static final String INFINITY = "infinity";
    public static final String SILK_TOUCH = "silk_touch";

    private Enchants() {
    }

    /** Resolves {@code sharpness} or {@code SHARPNESS} or {@code minecraft:sharpness}. */
    public static Enchantment byKey(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("minecraft:")) {
            key = key.substring("minecraft:".length());
        }
        try {
            return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
