package org.bukkit.entity;

public interface ArmorStand extends LivingEntity {
    void setVisible(boolean visible);
    boolean isVisible();
    void setMarker(boolean marker);
    void setSmall(boolean small);
    void setBasePlate(boolean basePlate);
    void setArms(boolean arms);
}
