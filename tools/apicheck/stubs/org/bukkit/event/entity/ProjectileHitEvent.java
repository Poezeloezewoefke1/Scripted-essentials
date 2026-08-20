package org.bukkit.event.entity;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;

public class ProjectileHitEvent extends EntityEvent {
    public ProjectileHitEvent(Entity what) { super(what); }
    @Override public Projectile getEntity() { throw new UnsupportedOperationException(); }
    public Block getHitBlock() { throw new UnsupportedOperationException(); }
    public Entity getHitEntity() { throw new UnsupportedOperationException(); }
}
