package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.util.Lore;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runs the forgotten identities of glitched vanilla mobs. Glitched mobs register themselves
 * when they load or are glitched; every few ticks each acts out what it now remembers being.
 */
public final class Glitches {
    private static final Identifier FALL_UP = Forgotten.id("falling_up");
    private static final Map<UUID, MobEntity> TRACKED = new HashMap<>();
    private static final Map<UUID, Integer> FUSES = new HashMap<>();

    private Glitches() {
    }

    public static GlitchKind of(Entity entity) {
        String id = entity.getAttached(ModAttachments.GLITCH);
        return id == null ? null : GlitchKind.byId(id);
    }

    public static void apply(MobEntity mob, GlitchKind kind) {
        mob.setAttached(ModAttachments.GLITCH, kind.id());
        if (kind == GlitchKind.FALLING_UP) {
            EntityAttributeInstance gravity = mob.getAttributeInstance(EntityAttributes.GRAVITY);
            if (gravity != null && !gravity.hasModifier(FALL_UP)) {
                gravity.addPersistentModifier(new EntityAttributeModifier(FALL_UP, -0.12,
                        EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }
        if (mob.getEntityWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.WITCH, mob.getX(), mob.getBodyY(0.5), mob.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
        }
        TRACKED.put(mob.getUuid(), mob);
    }

    /** Puts a creature back the way it is supposed to be. Returns whether it was glitched. */
    public static boolean heal(MobEntity mob) {
        if (of(mob) == null) {
            return false;
        }
        mob.removeAttached(ModAttachments.GLITCH);
        EntityAttributeInstance gravity = mob.getAttributeInstance(EntityAttributes.GRAVITY);
        if (gravity != null) {
            gravity.removeModifier(FALL_UP);
        }
        TRACKED.remove(mob.getUuid());
        FUSES.remove(mob.getUuid());
        return true;
    }

    public static void track(Entity entity) {
        if (entity instanceof MobEntity mob && of(mob) != null) {
            TRACKED.put(mob.getUuid(), mob);
        }
    }

    public static void untrack(Entity entity) {
        TRACKED.remove(entity.getUuid());
    }

    public static int count() {
        return TRACKED.size();
    }

    /** Called every tick from the server tick loop; each mob acts every fifth tick. */
    public static void tick(long time) {
        if (TRACKED.isEmpty()) {
            return;
        }
        List<MobEntity> mobs = new ArrayList<>(TRACKED.values());
        for (MobEntity mob : mobs) {
            if (mob.isRemoved() || !mob.isAlive()) {
                TRACKED.remove(mob.getUuid());
                continue;
            }
            if ((time + mob.getId()) % 5 != 0 || !(mob.getEntityWorld() instanceof ServerWorld world)) {
                continue;
            }
            GlitchKind kind = of(mob);
            if (kind == null) {
                TRACKED.remove(mob.getUuid());
                continue;
            }
            act(world, mob, kind, time);
        }
    }

    private static void act(ServerWorld world, MobEntity mob, GlitchKind kind, long time) {
        PlayerEntity player = world.getClosestPlayer(mob, 24);
        switch (kind) {
            case HERD -> herd(world, mob, player);
            case BLINKER -> {
                if (player != null && world.getRandom().nextInt(20) == 0) {
                    Vec3d behind = player.getEntityPos().subtract(player.getRotationVector().multiply(2.0));
                    mob.teleport(behind.x, player.getY(), behind.z, true);
                    world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                            SoundCategory.HOSTILE, 0.6f, 1.4f);
                }
            }
            case WALLWALKER -> {
                if (player != null && world.getRandom().nextInt(8) == 0) {
                    Vec3d dir = player.getEntityPos().subtract(mob.getEntityPos()).normalize().multiply(2.5);
                    BlockPos target = BlockPos.ofFloored(mob.getEntityPos().add(dir));
                    if (world.getBlockState(target).isAir() && world.getBlockState(target.up()).isAir()) {
                        mob.refreshPositionAndAngles(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, mob.getYaw(), mob.getPitch());
                    }
                }
            }
            case STARER -> {
                if (player != null) {
                    mob.getNavigation().stop();
                    mob.getLookControl().lookAt(player, 30f, 30f);
                    if (world.getRandom().nextInt(200) == 0 && player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                        String line = Palimpsests.randomLine(world.getRandom());
                        if (line != null) {
                            dev.scripted.forgotten.util.Texts.bar(sp, net.minecraft.text.Text.literal("<" + mob.getType().getName().getString() + "> ")
                                    .append(dev.scripted.forgotten.util.Texts.world(line)));
                        }
                    }
                }
            }
            case ARCHIVIST -> {
                if (world.getRandom().nextInt(40) == 0) {
                    BlockPos p = mob.getBlockPos().add(world.getRandom().nextInt(17) - 8, 0, world.getRandom().nextInt(17) - 8);
                    BlockPos top = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, p);
                    mob.teleport(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, true);
                    if (mob instanceof VillagerEntity && world.getRandom().nextInt(6) == 0) {
                        ItemStack book = Lore.book(world.getRandom());
                        mob.dropStack(world, book);
                    }
                }
            }
            case GENTLE -> {
                if (mob instanceof HostileEntity) {
                    mob.setTarget(null);
                }
            }
            case FALLING_UP -> {
                if (mob.getY() > world.getTopYInclusive() - 2) {
                    mob.discard();
                }
            }
        }
    }

    private static void herd(ServerWorld world, MobEntity mob, PlayerEntity player) {
        UUID id = mob.getUuid();
        Integer fuse = FUSES.get(id);
        if (fuse != null) {
            fuse -= 5;
            if (fuse % 10 == 0) {
                mob.playAmbientSound();
                world.spawnParticles(ParticleTypes.SMOKE, mob.getX(), mob.getBodyY(0.8), mob.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
            }
            if (fuse <= 0) {
                FUSES.remove(id);
                world.createExplosion(mob, mob.getX(), mob.getY(), mob.getZ(), 2.0f, World.ExplosionSourceType.MOB);
                mob.discard();
            } else {
                FUSES.put(id, fuse);
            }
            return;
        }
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        double d2 = mob.squaredDistanceTo(player);
        if (d2 < 2.5 * 2.5) {
            FUSES.put(id, 30);
            mob.getNavigation().stop();
            world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_CREEPER_PRIMED,
                    SoundCategory.HOSTILE, 0.3f, 0.5f);
        } else if (d2 < 16 * 16) {
            mob.getNavigation().startMovingTo(player, 1.0);
        }
    }
}
