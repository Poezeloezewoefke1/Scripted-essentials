package org.bukkit.entity;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface Entity extends PersistentDataHolder, Audience {
    UUID getUniqueId();
    String getName();
    EntityType getType();
    Location getLocation();
    World getWorld();
    boolean teleport(Location location);
    boolean teleport(Entity destination);
    void remove();
    boolean isValid();
    boolean isDead();
    void setCustomName(String name);
    void customName(Component name);
    Component customName();
    void setCustomNameVisible(boolean flag);
    boolean isCustomNameVisible();
    void setGravity(boolean gravity);
    void setInvulnerable(boolean invulnerable);
    boolean isInvulnerable();
    void setSilent(boolean silent);
    void setVelocity(Vector velocity);
    Vector getVelocity();
    void setGlowing(boolean glowing);
    void setFireTicks(int ticks);
    Set<String> getScoreboardTags();
    boolean addScoreboardTag(String tag);
    List<Entity> getNearbyEntities(double x, double y, double z);
    List<Entity> getPassengers();
    boolean addPassenger(Entity passenger);
}
