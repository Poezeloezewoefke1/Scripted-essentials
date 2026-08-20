package org.bukkit.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;

public class EntityDamageEvent extends EntityEvent implements Cancellable {
    public EntityDamageEvent(Entity what) { super(what); }
    public double getDamage() { throw new UnsupportedOperationException(); }
    public void setDamage(double damage) { }
    public double getFinalDamage() { throw new UnsupportedOperationException(); }
    public DamageCause getCause() { throw new UnsupportedOperationException(); }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }

    public enum DamageCause {
        CONTACT, ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, SUFFOCATION, FALL, FIRE,
        FIRE_TICK, MELTING, LAVA, DROWNING, BLOCK_EXPLOSION, ENTITY_EXPLOSION, VOID, LIGHTNING,
        SUICIDE, STARVATION, POISON, MAGIC, WITHER, FALLING_BLOCK, THORNS, DRAGON_BREATH,
        CUSTOM, FLY_INTO_WALL, HOT_FLOOR, CRAMMING, DRYOUT, FREEZE, SONIC_BOOM, KILL, WORLD_BORDER
    }
}
