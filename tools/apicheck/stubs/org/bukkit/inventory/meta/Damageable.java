package org.bukkit.inventory.meta;

public interface Damageable extends ItemMeta {
    boolean hasDamage();
    int getDamage();
    void setDamage(int damage);
}
