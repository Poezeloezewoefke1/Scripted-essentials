package dev.scripted.essentials.util;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.Locale;

/**
 * Looks enchantments up by their vanilla key.
 *
 * <p>Key lookup rather than {@code Enchantment.SHARPNESS}: the constant names were renamed in
 * 1.20.5 (DAMAGE_ALL became SHARPNESS, DURABILITY became UNBREAKING) while the registry keys have
 * never changed. Since 1.21 enchantments are data driven, so this goes through
 * {@link RegistryAccess} — {@code Registry.ENCHANTMENT} is deprecated — which also means a
 * datapack's custom enchantments resolve here just as vanilla ones do.
 */
public final class Enchants {

    // Protection and damage
    public static final String PROTECTION = "protection";
    public static final String SHARPNESS = "sharpness";
    public static final String FIRE_ASPECT = "fire_aspect";
    public static final String LOOTING = "looting";
    public static final String SWEEPING_EDGE = "sweeping_edge";

    // Movement and utility
    public static final String FEATHER_FALLING = "feather_falling";
    public static final String DEPTH_STRIDER = "depth_strider";
    public static final String SWIFT_SNEAK = "swift_sneak";
    public static final String RESPIRATION = "respiration";
    public static final String AQUA_AFFINITY = "aqua_affinity";

    // Tools and upkeep
    public static final String EFFICIENCY = "efficiency";
    public static final String FORTUNE = "fortune";
    public static final String SILK_TOUCH = "silk_touch";
    public static final String UNBREAKING = "unbreaking";
    public static final String MENDING = "mending";

    private Enchants() {
    }

    /** Resolves {@code sharpness}, {@code SHARPNESS} or {@code minecraft:sharpness}. */
    public static Enchantment byKey(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("minecraft:")) {
            key = key.substring("minecraft:".length());
        }
        try {
            return RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .get(NamespacedKey.minecraft(key));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return null;
        }
    }

    /** The highest level this server allows for an enchantment, or 1 if it is unknown. */
    public static int maxLevel(String name) {
        Enchantment enchantment = byKey(name);
        return enchantment == null ? 1 : enchantment.getMaxLevel();
    }
}
