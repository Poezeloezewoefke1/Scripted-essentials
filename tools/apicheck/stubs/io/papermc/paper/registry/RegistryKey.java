package io.papermc.paper.registry;

import org.bukkit.Keyed;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffectType;

public interface RegistryKey<T> {
    RegistryKey<Enchantment> ENCHANTMENT = null;
    RegistryKey<PotionEffectType> MOB_EFFECT = null;
    RegistryKey<Villager.Profession> VILLAGER_PROFESSION = null;
}
