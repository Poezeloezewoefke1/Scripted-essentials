package org.bukkit.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EntityDeathEvent extends EntityEvent {
    public EntityDeathEvent(Entity what) { super(what); }
    @Override public LivingEntity getEntity() { throw new UnsupportedOperationException(); }
    public List<ItemStack> getDrops() { throw new UnsupportedOperationException(); }
    public int getDroppedExp() { throw new UnsupportedOperationException(); }
    public void setDroppedExp(int exp) { }
}
