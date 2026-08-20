package org.bukkit;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffectType;

/** Stub of the keyed registry lookup, used instead of version-specific enum constants. */
public interface Registry<T extends Keyed> {
    Registry<Enchantment> ENCHANTMENT = null;
    Registry<PotionEffectType> EFFECT = null;
    Registry<Villager.Profession> VILLAGER_PROFESSION = null;

    T get(NamespacedKey key);
}
