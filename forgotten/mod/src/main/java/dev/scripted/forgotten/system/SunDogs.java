package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Sun Dogs: at low integrity, during the day, the sun spawns echoes of itself that patrol the
 * sky in slow circles. They are not meant to be fought. Standing beneath one in the open burns;
 * striking one destabilizes everything around it, and does not hurt it at all.
 */
public final class SunDogs {
    public static final String TAG = "forgotten_sun_dog";
    private static final Map<UUID, Patrol> PATROLS = new HashMap<>();

    private record Patrol(BlazeEntity dog, Vec3d center, double radius, double phase) {
    }

    private SunDogs() {
    }

    public static void roll(ServerWorld world, ServerPlayerEntity player) {
        if (!world.isDay() || PATROLS.size() >= 6 || world.getRandom().nextInt(600) != 0
                || !Reality.stage(world, player.getBlockPos()).atLeast(IntegrityStage.CONTRADICTION)
                || !world.isSkyVisible(player.getBlockPos().up())) {
            return;
        }
        BlazeEntity dog = EntityType.BLAZE.create(world, SpawnReason.EVENT);
        if (dog == null) {
            return;
        }
        Vec3d center = player.getEntityPos().add(0, 24, 0);
        dog.refreshPositionAndAngles(center.x + 16, center.y, center.z, 0f, 0f);
        dog.setCustomName(Text.literal("Sun Dog").formatted(Formatting.GOLD));
        dog.setNoGravity(true);
        dog.setGlowing(true);
        dog.setInvulnerable(false);
        dog.addCommandTag(TAG);
        dog.setPersistent();
        world.spawnEntity(dog);
        Texts.bar(player, Texts.glitch("There are two suns. Then three."));
    }

    public static void adopt(Entity entity) {
        if (entity instanceof BlazeEntity dog && entity.getCommandTags().contains(TAG) && !entity.getEntityWorld().isClient()) {
            dog.goalSelector.clear(goal -> true);
            dog.targetSelector.clear(goal -> true);
            dog.setNoGravity(true);
            double phase = (dog.getId() * 0.618) % (Math.PI * 2);
            PATROLS.put(dog.getUuid(), new Patrol(dog, dog.getEntityPos(), 16 + (dog.getId() % 8), phase));
        }
    }

    public static void tick(long now) {
        Iterator<Patrol> it = PATROLS.values().iterator();
        while (it.hasNext()) {
            Patrol p = it.next();
            BlazeEntity dog = p.dog();
            if (dog.isRemoved() || !(dog.getEntityWorld() instanceof ServerWorld world)) {
                it.remove();
                continue;
            }
            if (!world.isDay() && now % 20 == 0) {
                world.spawnParticles(ParticleTypes.FLAME, dog.getX(), dog.getY(), dog.getZ(), 30, 0.4, 0.4, 0.4, 0.05);
                dog.discard();
                it.remove();
                continue;
            }
            double angle = p.phase() + now / 80.0;
            Vec3d target = p.center().add(Math.cos(angle) * p.radius(), Math.sin(now / 37.0) * 2, Math.sin(angle) * p.radius());
            Vec3d step = target.subtract(dog.getEntityPos()).multiply(0.15);
            dog.setVelocity(step);
            dog.velocityDirty = true;
            if (now % 20 == 0) {
                for (ServerPlayerEntity player : world.getPlayers(pl -> !pl.isSpectator() && !pl.isCreative()
                        && Math.abs(pl.getX() - dog.getX()) < 5 && Math.abs(pl.getZ() - dog.getZ()) < 5 && pl.getY() < dog.getY())) {
                    if (world.isSkyVisible(player.getBlockPos().up())) {
                        player.setOnFireFor(3);
                        player.damage(world, world.getDamageSources().inFire(), 2f);
                    }
                }
            }
        }
    }

    public static boolean isSunDog(Entity entity) {
        return entity.getCommandTags().contains(TAG);
    }

    /**
     * Fabric damage hook. A Sun Dog cannot be hurt; anything that tries tears at the world
     * around it instead. Returns false to cancel the damage.
     */
    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!isSunDog(entity) || !(entity.getEntityWorld() instanceof ServerWorld world)) {
            return true;
        }
        if (source.getAttacker() == null) {
            return false; // fire, falling, the sun itself: none of it applies
        }
        BlockPos at = entity.getBlockPos();
        ChunkPos center = new ChunkPos(at);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = new BlockPos(center.getCenterX() + dx * 16, at.getY(), center.getCenterZ() + dz * 16);
                Reality.setIntegrity(world, p, Math.max(0, Reality.integrity(world, p) - 15));
            }
        }
        GlitchKind[] kinds = {GlitchKind.HERD, GlitchKind.BLINKER, GlitchKind.WALLWALKER, GlitchKind.FALLING_UP};
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, entity.getBoundingBox().expand(24),
                m -> m.isAlive() && !isSunDog(m) && !BioNodes.isNode(m))) {
            if (world.getRandom().nextInt(3) == 0) {
                Glitches.apply(mob, kinds[world.getRandom().nextInt(kinds.length)]);
            }
        }
        world.playSound(null, at, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 3f, 0.5f);
        world.spawnParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY(), entity.getZ(), 40, 1.5, 1.5, 1.5, 0.2);
        if (source.getAttacker() instanceof ServerPlayerEntity player) {
            Reality.debt(player, 10);
            Texts.bar(player, Texts.glitch("You struck the sun. The sky flickers."));
        }
        return false;
    }

    public static void clear() {
        PATROLS.clear();
    }
}
