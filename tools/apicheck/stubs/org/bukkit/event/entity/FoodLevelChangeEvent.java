package org.bukkit.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;

public class FoodLevelChangeEvent extends EntityEvent implements Cancellable {
    public FoodLevelChangeEvent(Entity what) { super(what); }
    public int getFoodLevel() { throw new UnsupportedOperationException(); }
    public void setFoodLevel(int level) { }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
}
