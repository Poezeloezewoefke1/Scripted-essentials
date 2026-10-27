package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Scheduler;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Unmaker's side of the Unraveling: Glitch Grenades, the Editor's temporary rewrites, the
 * Crash, and the Null Materials that only exist where the world has been unmade.
 */
public final class Unraveling {
    public static final int INVERT_TICKS = 200;
    public static final int EDIT_TICKS = 1200;
    /** Players whose controls are inverted, until the given server tick. */
    private static final Map<UUID, Long> INVERTED = new HashMap<>();
    private static final Map<UUID, Vec3d> LAST = new HashMap<>();
    /** Creatures the Editor has declared to have the properties of a chest. */
    private static final Map<UUID, SimpleInventory> CHESTS = new HashMap<>();

    private Unraveling() {
    }

    // ---- Glitch Grenades ----

    /** Everything in a radius forgets which properties are its own. */
    public static void scramble(ServerWorld world, Vec3d at, Entity thrower) {
        BlockPos center = BlockPos.ofFloored(at);
        Random random = world.getRandom();
        world.playSound(null, center, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1f, 1.6f);
        world.spawnParticles(ParticleTypes.WITCH, at.x, at.y + 0.5, at.z, 60, 2.0, 1.0, 2.0, 0.2);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.5, at.z, 40, 2.0, 1.0, 2.0, 0.4);

        // mobs swap AI
        GlitchKind[] kinds = GlitchKind.values();
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, new net.minecraft.util.math.Box(center).expand(4), MobEntity::isAlive)) {
            if (BioNodes.isNode(mob)) {
                continue; // compiled code is immune to having its mind swapped; that is the point of compiling it
            }
            Glitches.apply(mob, kinds[random.nextInt(kinds.length)]);
        }
        // blocks swap types
        List<BlockPos> solid = new ArrayList<>();
        for (BlockPos p : BlockPos.iterateOutwards(center, 3, 3, 3)) {
            BlockState s = world.getBlockState(p);
            if (!s.isAir() && !s.hasBlockEntity() && s.getHardness(world, p) >= 0 && s.getFluidState().isEmpty()) {
                solid.add(p.toImmutable());
            }
        }
        for (int i = 0; i + 1 < solid.size() && i < 24; i += 2) {
            BlockPos a = solid.get(random.nextInt(solid.size()));
            BlockPos b = solid.get(random.nextInt(solid.size()));
            BlockState sa = world.getBlockState(a);
            BlockState sb = world.getBlockState(b);
            world.setBlockState(a, sb, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            world.setBlockState(b, sa, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }
        // players' controls invert
        for (ServerPlayerEntity player : world.getEntitiesByClass(ServerPlayerEntity.class, new net.minecraft.util.math.Box(center).expand(4), p -> !p.isSpectator())) {
            invert(player, INVERT_TICKS);
        }
        Bosses.error(world, at, dev.scripted.forgotten.logic.boss.StackDepth.ErrorKind.GLITCH_GRENADE);
        Reality.setIntegrity(world, center, Math.max(0, Reality.integrity(world, center) - 8));
        Reality.paradox(world, center, 2);
        Block.dropStack(world, center, new ItemStack(ModItems.ENTROPY_SHARD, 1 + random.nextInt(2)));
        if (thrower instanceof ServerPlayerEntity player) {
            Reality.count(world, ChronicleEvent.ANOMALY_WITNESSED, "glitch_grenade");
            Reality.debt(player, 3);
        }
    }

    /** Inverts a player's controls for a while. */
    public static void invert(ServerPlayerEntity player, int ticks) {
        INVERTED.put(player.getUuid(), Reality.now(Reality.world(player).getServer()) + ticks);
        LAST.put(player.getUuid(), player.getEntityPos());
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, ticks, 0));
        Texts.bar(player, Texts.glitch("Your controls have been swapped with someone else's."));
    }

    /**
     * Inverted controls. The server cannot read a player's keys, so it watches where they move
     * each tick and pushes them twice as far the other way: forward becomes backward.
     */
    public static void tick(net.minecraft.server.MinecraftServer server, long now) {
        if (INVERTED.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Long>> it = INVERTED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
            if (player == null || now >= e.getValue()) {
                it.remove();
                LAST.remove(e.getKey());
                continue;
            }
            Vec3d pos = player.getEntityPos();
            Vec3d last = LAST.put(e.getKey(), pos);
            if (last == null || !player.isOnGround()) {
                continue;
            }
            double dx = pos.x - last.x;
            double dz = pos.z - last.z;
            if (dx * dx + dz * dz > 0.0004 && dx * dx + dz * dz < 1.0) {
                player.setVelocity(-dx * 2.0, player.getVelocity().y, -dz * 2.0);
                player.velocityDirty = true;
            }
        }
    }

    // ---- the Editor ----

    /**
     * Declares that a connected surface is made of something else, for a minute. Only blocks of
     * the same kind as the one pointed at, inside the same chunk, are rewritten.
     */
    public static int rewriteSurface(ServerWorld world, BlockPos start, BlockState into, int limit) {
        BlockState from = world.getBlockState(start);
        if (from.isAir() || from.hasBlockEntity() || from.getHardness(world, start) < 0 || from.equals(into)) {
            return 0;
        }
        ChunkPos chunk = new ChunkPos(start);
        List<BlockPos> changed = new ArrayList<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && changed.size() < limit) {
            BlockPos p = queue.poll();
            if (!world.getBlockState(p).equals(from) || !new ChunkPos(p).equals(chunk)) {
                continue;
            }
            changed.add(p);
            for (net.minecraft.util.math.Direction d : net.minecraft.util.math.Direction.values()) {
                BlockPos n = p.offset(d);
                if (seen.add(n)) {
                    queue.add(n);
                }
            }
        }
        for (BlockPos p : changed) {
            world.setBlockState(p, into, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }
        Scheduler.after(EDIT_TICKS, () -> {
            for (BlockPos p : changed) {
                if (world.getBlockState(p).equals(into)) {
                    world.setBlockState(p, from, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                }
            }
        });
        return changed.size();
    }

    /** The Editor has declared that this creature has the properties of a chest. */
    public static SimpleInventory chestOf(MobEntity mob) {
        SimpleInventory inv = CHESTS.get(mob.getUuid());
        if (inv == null) {
            inv = new SimpleInventory(27);
            CHESTS.put(mob.getUuid(), inv);
            UUID id = mob.getUuid();
            SimpleInventory held = inv;
            Scheduler.after(EDIT_TICKS, () -> {
                if (CHESTS.remove(id) != null) {
                    spill(mob, held);
                }
            });
        }
        return inv;
    }

    /** A creature stops being a chest (by time or by dying): what it held falls out. */
    public static void forgetChest(Entity entity) {
        SimpleInventory inv = CHESTS.remove(entity.getUuid());
        if (inv != null) {
            spill(entity, inv);
        }
    }

    private static void spill(Entity entity, SimpleInventory inv) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.removeStack(i);
            if (!stack.isEmpty()) {
                world.spawnEntity(new ItemEntity(world, entity.getX(), entity.getY() + 0.5, entity.getZ(), stack));
            }
        }
    }

    // ---- the Crash ----

    /**
     * The Unmaker's ritual. Begins in a chunk that has already fallen to Fragmentation and, over
     * ten seconds, unmakes a five-by-five chunk region permanently.
     */
    public static boolean crash(ServerWorld world, BlockPos at, ServerPlayerEntity player) {
        if (Reality.integrity(world, at) >= 25) {
            Texts.bar(player, Texts.warn("The world still holds together here. Unravel it further first."));
            return false;
        }
        ChunkPos center = new ChunkPos(at);
        RegionMemory region = Reality.region(world);
        world.playSound(null, at, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.MASTER, 2f, 0.5f);
        Texts.tell(player, Texts.glitch("FATAL: the region could not be loaded and was not saved."));
        for (int ring = 0; ring <= 2; ring++) {
            int r = ring;
            Scheduler.after(20 + ring * 80, () -> {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        ChunkPos c = new ChunkPos(center.x + dx, center.z + dz);
                        region.crash(c.toLong());
                        unmakeSurface(world, c);
                    }
                }
                world.playSound(null, at, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.MASTER, 1.5f, 0.4f + r * 0.2f);
                if (r == 2 && player.isAlive() && player.getEntityWorld() == world && new ChunkPos(player.getBlockPos()).equals(center)) {
                    // Whoever stands at the centre when the region finishes unmaking is in the Null Layer.
                    BlockPos top = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, at.add(6, 0, 6));
                    Bosses.summon(Bosses.Kind.COMPILER, world, top);
                    Texts.tell(player, Texts.glitch("You are inside the Null Layer. Something here is still running."));
                }
            });
        }
        Reality.debt(player, 60);
        Reality.chronicle(world, ChronicleEvent.ANOMALY_WITNESSED, at, player.getName().getString(), "crash");
        Reality.advance(player, "the_crash");
        return true;
    }

    /** Scars a crashed chunk: null holes and patches of Unremembered Things on the surface. */
    private static void unmakeSurface(ServerWorld world, ChunkPos chunk) {
        if (!world.isChunkLoaded(chunk.x, chunk.z)) {
            return;
        }
        Random random = world.getRandom();
        for (int i = 0; i < 40; i++) {
            int x = chunk.getStartX() + random.nextInt(16);
            int z = chunk.getStartZ() + random.nextInt(16);
            BlockPos top = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).down();
            BlockState s = world.getBlockState(top);
            if (s.hasBlockEntity() || s.getHardness(world, top) < 0) {
                continue;
            }
            BlockState into = random.nextInt(4) == 0 ? ModBlocks.NULL_BLOCK.getDefaultState() : ModBlocks.UNREMEMBERED_BLOCK.getDefaultState();
            world.setBlockState(top, into, Block.NOTIFY_ALL);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, x + 0.5, top.getY() + 1.2, z + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
        }
    }

    /** Null Materials: things that exist but should not. They only drop where integrity is zero. */
    public static void nullDrop(ServerWorld world, BlockPos pos, boolean creature) {
        if (Reality.integrity(world, pos) > 0) {
            return;
        }
        Random random = world.getRandom();
        int chance = creature ? 4 : 20;
        if (random.nextInt(chance) == 0) {
            Block.dropStack(world, pos, new ItemStack(ModItems.NULL_SHARD));
        }
    }

    public static void clear() {
        INVERTED.clear();
        LAST.clear();
        CHESTS.clear();
    }

    /** True if the creature is under a temporary Editor declaration. */
    public static boolean isChest(LivingEntity entity) {
        return CHESTS.containsKey(entity.getUuid());
    }

    static BlockState air() {
        return Blocks.AIR.getDefaultState();
    }
}
