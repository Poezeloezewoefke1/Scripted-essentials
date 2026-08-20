package org.bukkit.event.entity;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PlayerDeathEvent extends EntityDeathEvent {
    public PlayerDeathEvent(Entity what) { super(what); }
    @Override public Player getEntity() { throw new UnsupportedOperationException(); }
    public Component deathMessage() { throw new UnsupportedOperationException(); }
    public void deathMessage(Component message) { }
    public boolean getKeepInventory() { throw new UnsupportedOperationException(); }
    public void setKeepInventory(boolean keep) { }
    public boolean getKeepLevel() { throw new UnsupportedOperationException(); }
    public void setKeepLevel(boolean keep) { }
    public void setNewExp(int exp) { }
}
