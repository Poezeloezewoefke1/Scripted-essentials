package org.bukkit.event.entity;

import org.bukkit.entity.Entity;

public class EntityDamageByEntityEvent extends EntityDamageEvent {
    public EntityDamageByEntityEvent(Entity what) { super(what); }
    public Entity getDamager() { throw new UnsupportedOperationException(); }
}
