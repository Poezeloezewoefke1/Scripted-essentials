package org.bukkit.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;

public class ProjectileLaunchEvent extends EntityEvent implements Cancellable {
    public ProjectileLaunchEvent(Entity what) { super(what); }
    @Override public Projectile getEntity() { throw new UnsupportedOperationException(); }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
}
