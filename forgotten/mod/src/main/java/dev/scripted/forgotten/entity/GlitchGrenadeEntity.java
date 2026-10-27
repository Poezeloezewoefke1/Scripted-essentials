package dev.scripted.forgotten.entity;

import dev.scripted.forgotten.registry.ModEntities;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.system.Unraveling;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** A thrown Glitch Grenade. Where it lands, everything forgets which properties are its own. */
public class GlitchGrenadeEntity extends ThrownItemEntity {
    public GlitchGrenadeEntity(EntityType<? extends GlitchGrenadeEntity> type, World world) {
        super(type, world);
    }

    public GlitchGrenadeEntity(World world, LivingEntity owner, ItemStack stack) {
        super(ModEntities.GLITCH_GRENADE, owner, world, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GLITCH_GRENADE;
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (getEntityWorld() instanceof ServerWorld world && !isRemoved()) {
            Unraveling.scramble(world, hit.getPos(), getOwner());
            discard();
        }
    }
}
