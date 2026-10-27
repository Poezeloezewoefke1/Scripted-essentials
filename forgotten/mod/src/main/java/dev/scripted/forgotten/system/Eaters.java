package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.registry.ModAttachments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * The Possibility Eater (Hunger + Possibility) eats outcomes before they happen: within twelve
 * blocks of one, monsters are never born and nothing explodes.
 */
public final class Eaters {
    public static final double RADIUS = 12;
    /** Every loaded creeper and primed TNT: the only things that can be about to explode. */
    private static final java.util.Set<Entity> EXPLOSIVES = new java.util.LinkedHashSet<>();

    private Eaters() {
    }

    /** Explosives currently about to go off (a tick before they do). */
    public static java.util.List<Entity> aboutToExplode() {
        EXPLOSIVES.removeIf(Entity::isRemoved);
        return EXPLOSIVES.stream().filter(Genesis::aboutToExplode).toList();
    }

    public static void untrack(Entity entity) {
        EXPLOSIVES.remove(entity);
    }

    public static void clear() {
        EXPLOSIVES.clear();
    }

    /** A hostile creature just came into being near an Eater: it never did. */
    public static void onLoad(Entity entity, ServerWorld world) {
        if (entity instanceof net.minecraft.entity.mob.CreeperEntity || entity instanceof net.minecraft.entity.TntEntity) {
            EXPLOSIVES.add(entity);
        }
        if (entity.age > 0 || !(entity instanceof HostileEntity) || !entity.getCommandTags().isEmpty()
                || entity.hasAttached(ModAttachments.BOSS)) {
            return;
        }
        if (Reality.region(world).eaten(entity.getBlockPos(), RADIUS)) {
            entity.discard();
        }
    }

    /** Anything about to explode near an Eater is eaten first. */
    public static void tick(MinecraftServer server) {
        for (Entity e : aboutToExplode()) {
            if (!(e.getEntityWorld() instanceof ServerWorld world)) {
                continue;
            }
            if (Reality.region(world).eaten(e.getBlockPos(), RADIUS)) {
                world.spawnParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getBodyY(0.5), e.getZ(), 30, 0.4, 0.4, 0.4, 0.05);
                world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, SoundCategory.BLOCKS, 1f, 0.6f);
                e.discard();
            }
        }
    }
}
