package org.bukkit.inventory.meta;

import org.bukkit.Color;
import org.bukkit.potion.PotionEffect;

public interface PotionMeta extends ItemMeta {
    boolean addCustomEffect(PotionEffect effect, boolean overwrite);
    boolean clearCustomEffects();
    void setColor(Color color);
}
