package org.bukkit.entity;

public interface Damageable extends Entity {
    void damage(double amount);
    void damage(double amount, Entity source);
    double getHealth();
    void setHealth(double health);
    double getAbsorptionAmount();
    void setAbsorptionAmount(double amount);
    /** Deprecated in Bukkit in favour of attributes, but stable across every 1.x release. */
    @Deprecated double getMaxHealth();
    @Deprecated void setMaxHealth(double health);
    @Deprecated void resetMaxHealth();
}
