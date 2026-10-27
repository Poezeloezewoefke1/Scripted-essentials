package dev.scripted.forgotten.registry;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.entity.GlitchGrenadeEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

/**
 * The few entity types the mod adds. Most of its creatures are vanilla mobs with rewritten
 * memories (see ModAttachments); only things with no vanilla equivalent get a type here.
 */
public final class ModEntities {
    public static final EntityType<GlitchGrenadeEntity> GLITCH_GRENADE = register("glitch_grenade",
            EntityType.Builder.<GlitchGrenadeEntity>create(GlitchGrenadeEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f).maxTrackingRange(4).trackingTickInterval(10));

    private ModEntities() {
    }

    private static <T extends net.minecraft.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Forgotten.id(name));
        return Registry.register(Registries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void init() {
        // Static initialisation registers everything above.
    }
}
