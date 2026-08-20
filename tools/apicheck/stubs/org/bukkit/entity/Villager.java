package org.bukkit.entity;

import org.bukkit.Keyed;

public interface Villager extends LivingEntity {
    Profession getProfession();
    void setProfession(Profession profession);
    int getVillagerLevel();
    void setVillagerLevel(int level);

    interface Profession extends Keyed {
    }
}
