package org.bukkit.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class EntityEvent extends Event {
    public EntityEvent(Entity what) { }
    public Entity getEntity() { throw new UnsupportedOperationException(); }
    @Override public HandlerList getHandlers() { throw new UnsupportedOperationException(); }
}
