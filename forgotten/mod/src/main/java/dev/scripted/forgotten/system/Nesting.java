package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.dimension.Dimensions;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.nesting.Depths;
import dev.scripted.forgotten.logic.nesting.Mass;
import dev.scripted.forgotten.logic.nesting.NestingInstability;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.BlockEntityTickInvoker;

import java.util.ArrayList;
import java.util.List;

/**
 * The Matryoshka. A Frame Anchor opens a copy of the world one depth down, in a region of its
 * own; inside, time runs faster - furnaces, crops, everything that ticks - and the copy is a
 * little more wrong at each depth. Carrying mass back up costs.
 */
public final class Nesting {
    /** Most extra block-entity ticks run per game tick, across a whole depth world. */
    public static final int TICK_BUDGET = 4000;
    /** Most extra random ticks per player per game tick. */
    public static final int RANDOM_BUDGET = 384;

    private Nesting() {
    }

    // ---- anchors ----

    public static WorldMemory.FrameRecord register(ServerWorld world, BlockPos pos) {
        WorldMemory memory = Reality.memory(world.getServer());
        for (WorldMemory.FrameRecord f : memory.frames.values()) {
            if (f.pos() == pos.asLong() && f.dimension().equals(world.getRegistryKey().getValue().toString())) {
                return f;
            }
        }
        WorldMemory.FrameRecord frame = new WorldMemory.FrameRecord(memory.nextFrameId++, world.getRegistryKey().getValue().toString(),
                pos.asLong(), Dimensions.depthOf(world));
        memory.frames.put(frame.id(), frame);
        memory.markDirty();
        return frame;
    }

    public static void unregister(ServerWorld world, BlockPos pos) {
        WorldMemory memory = Reality.memory(world.getServer());
        String dim = world.getRegistryKey().getValue().toString();
        if (memory.frames.values().removeIf(f -> f.pos() == pos.asLong() && f.dimension().equals(dim))) {
            memory.markDirty();
        }
    }

    /** Using a Frame Anchor: go one depth down into the region it owns. */
    public static boolean descend(ServerPlayerEntity player, BlockPos anchor) {
        ServerWorld world = Reality.world(player);
        int depth = Dimensions.depthOf(world);
        if (depth >= Depths.MAX) {
            Texts.bar(player, Texts.warn("There is no copy of this copy. This is the Scratch Layer."));
            return false;
        }
        ServerWorld below = world.getServer().getWorld(Dimensions.depth(depth + 1));
        if (below == null) {
            return false;
        }
        WorldMemory.FrameRecord frame = register(world, anchor);
        PlayerMemory memory = Reality.player(player);
        long now = Reality.now(world.getServer());
        boolean anchored = Reality.region(world).anchored(ChunkPos.toLong(anchor));
        memory.instability += NestingInstability.onDescend(depth + 1, anchored, now - memory.lastDescent);
        memory.lastDescent = now;
        memory.depthStack.add(Rifts.point(world, Vec3d.ofBottomCenter(anchor.up())));

        int x = Depths.regionCenterX(frame.id()) + (anchor.getX() & 63);
        int z = anchor.getZ() & 1023;
        Vec3d at = Travel.surface(below, x, z);
        Travel.teleport(player, below, at, player.getYaw(), player.getPitch());
        BlockPos home = BlockPos.ofFloored(at).add(2, 0, 0);
        if (!below.getBlockState(home).isOf(ModBlocks.FRAME_ANCHOR)) {
            below.setBlockState(home, ModBlocks.FRAME_ANCHOR.getDefaultState(), Block.NOTIFY_ALL);
            register(below, home);
        }
        below.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1f, 0.6f + depth * 0.1f);
        Texts.tell(player, Texts.discovery("DEPTH " + (depth + 1) + "  (time x" + Depths.timeFactor(depth + 1) + ")"));
        Texts.tell(player, Texts.world(Depths.copyError(depth + 1)));
        Reality.chronicle(below, ChronicleEvent.DIMENSION_ENTERED, home, player.getName().getString(), "depth_" + (depth + 1));
        Reality.advance(player, depth + 1 >= Depths.MAX ? "scratch_layer" : "depth");
        symptoms(player, memory);
        return true;
    }

    /** Crouch-using a Frame Anchor inside a depth: go back up, paying for what you carry. */
    public static boolean ascend(ServerPlayerEntity player) {
        ServerWorld world = Reality.world(player);
        PlayerMemory memory = Reality.player(player);
        if (!Dimensions.isDepth(world) || memory.depthStack.isEmpty()) {
            return false;
        }
        double mass = carried(player);
        switch (Mass.ascend(mass, 1)) {
            case EJECTED -> {
                for (int i = 0; i < player.getInventory().size(); i++) {
                    ItemStack stack = player.getInventory().removeStack(i);
                    if (!stack.isEmpty() && Mass.of(Registries.ITEM.getId(stack.getItem()).toString()) > 0) {
                        player.dropItem(stack, false);
                    } else if (!stack.isEmpty()) {
                        player.getInventory().setStack(i, stack);
                    }
                }
                Texts.bar(player, Texts.warn("Too heavy to rise. Mass " + (int) mass + ". Everything with weight stays here."));
                return false;
            }
            case CRUSHED -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6000, 2));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 6000, 1));
                Texts.tell(player, Texts.warn("Mass Debt: you carried " + (int) mass + " up a layer. It will take a while to stop feeling it."));
            }
            case FREE -> {
            }
        }
        String back = memory.depthStack.remove(memory.depthStack.size() - 1);
        Rifts.goTo(player, back);
        memory.instability = Math.max(0, memory.instability - 4);
        return true;
    }

    public static double carried(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        double total = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                total += Mass.of(Registries.ITEM.getId(stack.getItem()).toString()) * stack.getCount();
            }
        }
        return total;
    }

    static void symptoms(ServerPlayerEntity player, PlayerMemory memory) {
        switch (memory.symptom()) {
            case ECHO_STEPS -> Texts.tell(player, Texts.world("You hear your own footsteps, a little late."));
            case MIRRORED_BLOCKS -> Texts.tell(player, Texts.warn("The layers are slipping. Things you build appear in the layer above, backwards."));
            case UNWRAPPER -> {
                Texts.tell(player, Texts.glitch("Something has noticed how deep you are. It is coming to open the first frame."));
                WorldMemory frames = Reality.memory(player.getEntityWorld().getServer());
                frames.frames.values().stream().min(java.util.Comparator.comparingInt(WorldMemory.FrameRecord::id)).ifPresent(first -> {
                    var key = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD,
                            net.minecraft.util.Identifier.of(first.dimension()));
                    ServerWorld home = player.getEntityWorld().getServer().getWorld(key);
                    if (home != null) {
                        BlockPos anchor = BlockPos.fromLong(first.pos());
                        BlockPos at = home.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, anchor.add(24, 0, 0));
                        Bosses.summon(Bosses.Kind.UNWRAPPER, home, at);
                    }
                });
            }
            default -> {
            }
        }
    }

    // ---- time dilation ----

    /** Runs a depth world's block entities and random ticks extra times, per its time factor. */
    public static void dilate(ServerWorld world) {
        int depth = Dimensions.depthOf(world);
        if (depth <= 0 || world.getPlayers().isEmpty()) {
            return;
        }
        int extra = Depths.extraBlockEntityTicks(depth, 7);
        List<BlockEntityTickInvoker> tickers = new ArrayList<>(world.blockEntityTickers);
        int budget = TICK_BUDGET;
        for (int round = 0; round < extra && budget > 0; round++) {
            for (BlockEntityTickInvoker ticker : tickers) {
                if (budget-- <= 0) {
                    break;
                }
                if (!ticker.isRemoved() && world.isChunkLoaded(ticker.getPos())) {
                    ticker.tick();
                }
            }
        }
        Random random = world.getRandom();
        int perPlayer = Math.min(RANDOM_BUDGET, Depths.extraRandomTicks(depth) * 4);
        for (ServerPlayerEntity player : world.getPlayers()) {
            BlockPos center = player.getBlockPos();
            for (int i = 0; i < perPlayer; i++) {
                BlockPos p = center.add(random.nextInt(97) - 48, random.nextInt(33) - 16, random.nextInt(97) - 48);
                if (!world.isChunkLoaded(p)) {
                    continue;
                }
                BlockState s = world.getBlockState(p);
                if (s.hasRandomTicks()) {
                    s.randomTick(world, p, random);
                }
            }
        }
    }

    // ---- copy errors ----

    /** The first time a player stands in a depth chunk, the copy's mistakes are written into it. */
    public static void copyErrors(ServerWorld world, ServerPlayerEntity player) {
        int depth = Dimensions.depthOf(world);
        if (depth <= 0) {
            return;
        }
        RegionMemory region = Reality.region(world);
        ChunkPos center = player.getChunkPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos c = new ChunkPos(center.x + dx, center.z + dz);
                RegionMemory.ChunkRecord record = region.chunk(c.toLong());
                if (record.copied || !world.isChunkLoaded(c.x, c.z)) {
                    continue;
                }
                record.copied = true;
                region.markDirty();
                applyCopyErrors(world, c, depth);
            }
        }
    }

    /** Returns how many blocks were changed. */
    public static int applyCopyErrors(ServerWorld world, ChunkPos chunk, int depth) {
        double rate = Depths.errorRate(depth);
        Random random = Random.create(chunk.toLong() * 31 + depth);
        int changed = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (random.nextDouble() >= rate) {
                    continue;
                }
                BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                        new BlockPos(chunk.getStartX() + x, 0, chunk.getStartZ() + z)).down();
                BlockState s = world.getBlockState(top);
                BlockState into = error(s, depth, random);
                if (into != null && !s.hasBlockEntity()) {
                    world.setBlockState(top, into, Block.NOTIFY_LISTENERS);
                    changed++;
                }
            }
        }
        return changed;
    }

    static BlockState error(BlockState s, int depth, Random random) {
        if (s.isAir()) {
            return null;
        }
        return switch (Math.min(depth, 5)) {
            case 1 -> s.isOf(Blocks.GRASS_BLOCK) ? Blocks.MOSS_BLOCK.getDefaultState() : Blocks.COARSE_DIRT.getDefaultState();
            case 2 -> s.isIn(BlockTags.LEAVES) ? Blocks.AIR.getDefaultState() : Blocks.ROOTED_DIRT.getDefaultState();
            case 3 -> new BlockState[]{Blocks.SAND.getDefaultState(), Blocks.SNOW_BLOCK.getDefaultState(),
                    Blocks.RED_TERRACOTTA.getDefaultState(), Blocks.MYCELIUM.getDefaultState()}[random.nextInt(4)];
            case 4 -> s.isOf(Blocks.WATER) ? Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState()
                    : new BlockState[]{Blocks.IRON_BLOCK.getDefaultState(), Blocks.HAY_BLOCK.getDefaultState(),
                    Blocks.SPONGE.getDefaultState()}[random.nextInt(3)];
            default -> random.nextInt(3) == 0 ? Blocks.WHITE_WOOL.getDefaultState() : Blocks.TINTED_GLASS.getDefaultState();
        };
    }

}
