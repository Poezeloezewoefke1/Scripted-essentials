package org.bukkit.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;

public class EntityPickupItemEvent extends EntityEvent implements Cancellable {
    public EntityPickupItemEvent(Entity what) { super(what); }
    public Item getItem() { throw new UnsupportedOperationException(); }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
}
