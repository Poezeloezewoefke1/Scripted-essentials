package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.block.ChamberBlock;
import dev.scripted.forgotten.block.entity.MemoryVaultBlockEntity;
import dev.scripted.forgotten.registry.ModComponents;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.chunk.BlockEntityTickInvoker;

import java.util.ArrayList;
import java.util.List;

/**
 * Reality Architecture: the chambers of a base built as its own universe. Each chamber works
 * within six blocks of itself, and only while a Core of Identity stands within thirty-two.
 */
public final class Architecture {
    /** Device code for the Memory Vault (chambers use their Kind ordinal). */
    public static final int VAULT = 10;
    public static final double CHAMBER_RADIUS = 6;
    public static final double CORE_RANGE = 32;
    private static final Identifier CHAMBER_GRAVITY = Forgotten.id("chamber_gravity");

    private Architecture() {
    }

    static boolean powered(RegionMemory region, BlockPos chamber) {
        return !region.devicesNear(chamber, ChamberBlock.Kind.CORE.ordinal(), CORE_RANGE).isEmpty();
    }

    public static String describe(ServerWorld world, BlockPos pos, ChamberBlock.Kind kind) {
        RegionMemory region = Reality.region(world);
        if (kind == ChamberBlock.Kind.CORE) {
            int chambers = 0;
            for (ChamberBlock.Kind k : ChamberBlock.Kind.values()) {
                if (k != ChamberBlock.Kind.CORE) {
                    chambers += region.devicesNear(pos, k.ordinal(), CORE_RANGE).size();
                }
            }
            return "This is the centre of something. " + chambers + " chambers answer to it.";
        }
        return powered(region, pos) ? "Time runs four times as fast here." : "It needs a Core of Identity within 32 blocks.";
    }

    // ---- per tick ----

    public static void tick(MinecraftServer server, long now) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = Reality.world(player);
            RegionMemory region = Reality.region(world);
            BlockPos at = player.getBlockPos();
            for (BlockPos chamber : region.devicesNear(at, ChamberBlock.Kind.TEMPORAL.ordinal(), 64)) {
                if (powered(region, chamber)) {
                    temporal(world, chamber);
                }
            }
            if (now % 10 == 0) {
                gravity(world, player, region);
            }
            if (now % 100 == 0) {
                for (BlockPos vault : region.devicesNear(at, VAULT, 64)) {
                    reconstruct(world, region, vault);
                }
            }
        }
    }

    /** The Temporal Chamber: everything within it ticks three extra times. */
    static void temporal(ServerWorld world, BlockPos chamber) {
        List<BlockEntityTickInvoker> tickers = new ArrayList<>(world.blockEntityTickers);
        double r2 = CHAMBER_RADIUS * CHAMBER_RADIUS;
        for (BlockEntityTickInvoker ticker : tickers) {
            if (!ticker.isRemoved() && ticker.getPos().getSquaredDistance(chamber) <= r2) {
                for (int i = 0; i < 3; i++) {
                    ticker.tick();
                }
            }
        }
        Random random = world.getRandom();
        for (int i = 0; i < 24; i++) {
            BlockPos p = chamber.add(random.nextInt(13) - 6, random.nextInt(13) - 6, random.nextInt(13) - 6);
            BlockState s = world.getBlockState(p);
            if (s.hasRandomTicks()) {
                s.randomTick(world, p, random);
            }
        }
    }

    /** The Gravity Chamber: within it, things fall the way it was set. */
    static void gravity(ServerWorld world, ServerPlayerEntity player, RegionMemory region) {
        List<BlockPos> chambers = region.devicesNear(player.getBlockPos(), ChamberBlock.Kind.GRAVITY.ordinal(), 48);
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(32), Entity::isAlive)) {
            double wanted = 0;
            for (BlockPos c : chambers) {
                if (c.getSquaredDistance(e.getEntityPos()) <= CHAMBER_RADIUS * CHAMBER_RADIUS && powered(region, c)) {
                    BlockState s = world.getBlockState(c);
                    int mode = s.contains(ChamberBlock.MODE) ? s.get(ChamberBlock.MODE) : 0;
                    wanted = mode == 1 ? -0.12 : mode == 2 ? -0.08 : 0;
                    break;
                }
            }
            EntityAttributeInstance g = e.getAttributeInstance(EntityAttributes.GRAVITY);
            if (g == null) {
                continue;
            }
            EntityAttributeModifier current = g.getModifier(CHAMBER_GRAVITY);
            if (current != null && current.value() == wanted) {
                continue;
            }
            g.removeModifier(CHAMBER_GRAVITY);
            if (wanted != 0) {
                g.addTemporaryModifier(new EntityAttributeModifier(CHAMBER_GRAVITY, wanted, EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    /** The Memory Vault spends one memory to reconstruct up to eight lost blocks nearby. */
    public static void reconstruct(ServerWorld world, RegionMemory region, BlockPos vault) {
        if (!(world.getBlockEntity(vault) instanceof MemoryVaultBlockEntity entity)) {
            return;
        }
        int slot = -1;
        for (int i = 0; i < entity.memories.size(); i++) {
            if (entity.memories.getStack(i).isOf(ModItems.MEMORY)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            return;
        }
        int restored = 0;
        ChunkPos center = new ChunkPos(vault);
        for (int dx = -1; dx <= 1 && restored < 8; dx++) {
            for (int dz = -1; dz <= 1 && restored < 8; dz++) {
                RegionMemory.ChunkRecord r = region.peek(ChunkPos.toLong(center.x + dx, center.z + dz));
                if (r == null) {
                    continue;
                }
                List<RegionMemory.Remembered> lost = new ArrayList<>(r.ruins);
                lost.addAll(r.mined);
                for (RegionMemory.Remembered m : lost) {
                    if (restored >= 8) {
                        break;
                    }
                    if (m.blockPos().getSquaredDistance(vault) <= 16 * 16 && world.getBlockState(m.blockPos()).isAir()
                            && !m.state().hasBlockEntity()) {
                        world.setBlockState(m.blockPos(), m.state(), Block.NOTIFY_ALL);
                        r.ruins.remove(m);
                        r.mined.remove(m);
                        restored++;
                    }
                }
            }
        }
        if (restored > 0) {
            entity.memories.getStack(slot).decrement(1);
            region.markDirty();
            world.spawnParticles(ParticleTypes.ENCHANT, vault.getX() + 0.5, vault.getY() + 1.2, vault.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.6);
        }
    }

    // ---- the Distance Chamber ----

    static String key(ServerWorld world, BlockPos pos) {
        return Rifts.point(world, Vec3d.of(pos));
    }

    /** Join two Distance Chambers with an ender pearl: the first use marks, the second links. */
    public static void link(ServerWorld world, ServerPlayerEntity player, BlockPos pos, ItemStack held) {
        WorldMemory memory = Reality.memory(world.getServer());
        String here = key(world, pos);
        if (!held.isOf(Items.ENDER_PEARL)) {
            String there = memory.links.get(here);
            Texts.bar(player, Texts.world(there == null ? "Unjoined. Use an ender pearl on two doors to join them." : "Joined to " + there + "."));
            return;
        }
        String marked = held.get(ModComponents.LINK);
        if (marked == null || marked.equals(here)) {
            held.set(ModComponents.LINK, here);
            Texts.bar(player, Texts.world("The pearl remembers this door. Use it on another."));
            return;
        }
        memory.links.put(here, marked);
        memory.links.put(marked, here);
        memory.markDirty();
        held.decrementUnlessCreative(1, player);
        world.playSound(null, pos, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1f, 0.6f);
        Texts.bar(player, Texts.discovery("Two doors, one distance: none."));
    }

    public static void unlink(ServerWorld world, BlockPos pos) {
        WorldMemory memory = Reality.memory(world.getServer());
        String here = key(world, pos);
        String there = memory.links.remove(here);
        if (there != null) {
            memory.links.remove(there);
            memory.markDirty();
        }
    }

    /** Stepping onto a joined door steps you out of its partner. */
    public static void step(ServerWorld world, ServerPlayerEntity player, BlockPos pos) {
        String there = Reality.memory(world.getServer()).links.get(key(world, pos));
        if (there == null || !powered(Reality.region(world), pos) || Rifts.cooling(player, Reality.now(world.getServer()))) {
            return;
        }
        String[] f = there.split("\\|");
        if (f.length == 4) {
            Rifts.goTo(player, f[0] + "|" + f[1] + "|" + (Integer.parseInt(f[2]) + 1) + "|" + f[3]);
        }
    }
}
