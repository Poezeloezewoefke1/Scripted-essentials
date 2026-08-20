package org.bukkit.enchantments;

import org.bukkit.Keyed;

public interface Enchantment extends Keyed {
    int getMaxLevel();
    int getStartLevel();
}
