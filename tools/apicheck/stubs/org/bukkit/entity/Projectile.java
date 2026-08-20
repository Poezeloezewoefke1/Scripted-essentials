package org.bukkit.entity;

public interface Projectile extends Entity {
    ProjectileSource getShooter();
    void setShooter(ProjectileSource source);
}
