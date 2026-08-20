package org.bukkit.entity;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.Set;

public interface LivingEntity extends Damageable, ProjectileSource {
    boolean addPotionEffect(PotionEffect effect);
    boolean addPotionEffects(Collection<PotionEffect> effects);
    void removePotionEffect(PotionEffectType type);
    boolean hasPotionEffect(PotionEffectType type);
    PotionEffect getPotionEffect(PotionEffectType type);
    Collection<PotionEffect> getActivePotionEffects();
    EntityEquipment getEquipment();
    void setAI(boolean ai);
    boolean hasAI();
    void setCollidable(boolean collidable);
    void setRemoveWhenFarAway(boolean remove);
    Location getEyeLocation();
    Block getTargetBlockExact(int maxDistance);
    Entity getTargetEntity(int maxDistance);
    void setCanPickupItems(boolean pickup);
    Player getKiller();
}
